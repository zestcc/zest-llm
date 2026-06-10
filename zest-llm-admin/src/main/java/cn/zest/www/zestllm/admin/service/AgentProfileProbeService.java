package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmMcpServerDO;
import cn.zest.www.zestllm.admin.model.entity.LlmModelRouteDO;
import cn.zest.www.zestllm.admin.model.entity.LlmPromptVersionDO;
import cn.zest.www.zestllm.admin.model.entity.LlmProviderPresetDO;
import cn.zest.www.zestllm.admin.model.request.AgentProfileProbeRequest;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeCheckVO;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeResultVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmMcpServerRepo;
import cn.zest.www.zestllm.admin.repo.LlmModelRouteRepo;
import cn.zest.www.zestllm.admin.repo.LlmPromptVersionRepo;
import cn.zest.www.zestllm.admin.repo.LlmProviderPresetRepo;
import cn.zest.www.zestllm.spi.cache.CachedPolicy;
import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import cn.zest.www.zestllm.spi.profile.OutboundAuthConfig;
import cn.zest.www.zestllm.spi.profile.ToolDefinition;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import cn.zest.www.zestllm.spi.tool.McpToolAdapter;
import cn.zest.www.zestllm.spi.tool.McpToolListRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentProfileProbeService {

    private static final int PROBE_CONNECT_MS = 3000;
    private static final int PROBE_READ_MS = 15000;

    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmAppRepo appRepo;
    private final LlmAgentProfileRepo agentProfileRepo;
    private final LlmPromptVersionRepo promptVersionRepo;
    private final LlmModelRouteRepo modelRouteRepo;
    private final LlmProviderPresetRepo providerPresetRepo;
    private final LlmMcpServerRepo mcpServerRepo;
    private final AgentProfileResolver agentProfileResolver;
    private final SecretResolver secretResolver;
    private final McpToolAdapter mcpToolAdapter;
    private final ObjectMapper objectMapper;
    private final AgentProfileProbeRecordService probeRecordService;

    public AgentProfileProbeResultVO probePublished(String taskCode, AgentProfileProbeRequest request) {
        return probePublished(taskCode, request, AgentProfileProbeRecordService.SOURCE_MANUAL);
    }

    public AgentProfileProbeResultVO probePublished(String taskCode, AgentProfileProbeRequest request, String probeSource) {
        LlmAiTaskDefDO task = requireTask(taskCode);
        LlmAgentProfileDO profile = agentProfileRepo.findPublishedByTaskId(task.getId())
                .orElseThrow(() -> new BusinessException("PROFILE_NOT_PUBLISHED", "该作业尚无已发布的 Profile"));
        return persistProbe(task, profile, request, probeSource);
    }

    public AgentProfileProbeResultVO probeVersion(String taskCode, String version, AgentProfileProbeRequest request) {
        LlmAiTaskDefDO task = requireTask(taskCode);
        LlmAgentProfileDO profile = agentProfileRepo.findByTaskIdAndVersion(task.getId(), version)
                .orElseThrow(() -> new BusinessException("PROFILE_NOT_FOUND", "Profile 版本不存在: " + version));
        return persistProbe(task, profile, request, AgentProfileProbeRecordService.SOURCE_MANUAL);
    }

    public int probeAllPublished(boolean smokeTest, String probeSource) {
        int count = 0;
        AgentProfileProbeRequest request = new AgentProfileProbeRequest();
        request.setSmokeTest(smokeTest);
        for (LlmAgentProfileDO profile : agentProfileRepo.findAllPublished()) {
            LlmAiTaskDefDO task = taskDefRepo.findById(profile.getTaskId()).orElse(null);
            if (task == null) {
                continue;
            }
            try {
                persistProbe(task, profile, request, probeSource);
                count++;
            } catch (Exception ex) {
                log.warn("Scheduled agent probe failed taskCode={} version={}", task.getCode(), profile.getVersion(), ex);
            }
        }
        return count;
    }

    private AgentProfileProbeResultVO persistProbe(LlmAiTaskDefDO task, LlmAgentProfileDO profile,
                                                   AgentProfileProbeRequest request, String probeSource) {
        AgentProfileProbeRequest req = request != null ? request : new AgentProfileProbeRequest();
        AgentProfileProbeResultVO result = probeProfile(task, profile, req);
        return probeRecordService.save(task.getId(), result, req.isSmokeTest(), probeSource);
    }

    private AgentProfileProbeResultVO probeProfile(LlmAiTaskDefDO task, LlmAgentProfileDO profile,
                                                   AgentProfileProbeRequest request) {
        long start = System.currentTimeMillis();
        List<AgentProfileProbeCheckVO> checks = new ArrayList<>();
        AgentProfileProbeRequest req = request != null ? request : new AgentProfileProbeRequest();

        checks.add(checkPrompt(task));
        checks.add(checkModelRoute(task));

        AgentProfileDocument document = null;
        try {
            document = agentProfileResolver.parseProfile(profile.getProfileJson(), null);
            checks.add(critical("profile-json", "CONFIG", true, "Profile JSON 合法 · apiVersion=" + document.getApiVersion()));
        } catch (Exception ex) {
            checks.add(critical("profile-json", "CONFIG", false, ex.getMessage()));
        }

        if (document != null) {
            checks.add(checkProviderPreset(document, profile));
            checks.addAll(checkOutboundSecret(document));
            CachedPolicy policy = null;
            try {
                LlmAppDO app = resolveApp(task, req.getAppKey());
                policy = agentProfileResolver.resolve(app, task, profile, "probe_" + task.getCode());
                checks.add(configCheck("policy-resolve", true,
                        "策略解析成功 · model=" + policy.getPrimaryModel()
                                + " gateway=" + policy.getGatewayBaseUrl()));
            } catch (Exception ex) {
                checks.add(critical("policy-resolve", "CONFIG", false, ex.getMessage()));
            }
            if (policy != null && StringUtils.hasText(policy.getGatewayBaseUrl())) {
                checks.add(probeGatewayHealth(policy.getGatewayBaseUrl(), resolveGatewayApiKey(policy)));
            } else {
                checks.add(critical("gateway-health", "CONNECTIVITY", false, "未解析到 gatewayBaseUrl"));
            }
            checks.addAll(probeMcpTools(document));
            if (req.isSmokeTest() && policy != null && StringUtils.hasText(policy.getPrimaryModel())) {
                checks.add(smokeInvoke(policy));
            }
        }

        boolean allCriticalUp = checks.stream().filter(AgentProfileProbeCheckVO::isCritical).allMatch(AgentProfileProbeCheckVO::isUp);
        boolean anyCriticalDown = checks.stream().filter(AgentProfileProbeCheckVO::isCritical).anyMatch(c -> !c.isUp());
        boolean anyNonCriticalDown = checks.stream().filter(c -> !c.isCritical()).anyMatch(c -> !c.isUp());

        String overallStatus;
        if (anyCriticalDown) {
            overallStatus = "UNAVAILABLE";
        } else if (anyNonCriticalDown) {
            overallStatus = "DEGRADED";
        } else {
            overallStatus = "READY";
        }

        return AgentProfileProbeResultVO.builder()
                .taskCode(task.getCode())
                .profileVersion(profile.getVersion())
                .profileStatus(profile.getStatus())
                .overallStatus(overallStatus)
                .ready(allCriticalUp)
                .latencyMs(System.currentTimeMillis() - start)
                .checks(checks)
                .build();
    }

    private AgentProfileProbeCheckVO checkPrompt(LlmAiTaskDefDO task) {
        Optional<LlmPromptVersionDO> prompt = promptVersionRepo.findPublishedByTaskId(task.getId());
        if (prompt.isEmpty()) {
            return critical("prompt-published", "CONFIG", false, "无已发布 Prompt");
        }
        return critical("prompt-published", "CONFIG", true,
                "Prompt " + prompt.get().getVersion() + " 已发布");
    }

    private AgentProfileProbeCheckVO checkModelRoute(LlmAiTaskDefDO task) {
        Optional<LlmModelRouteDO> route = modelRouteRepo.findActiveByTaskId(task.getId());
        if (route.isEmpty()) {
            return critical("model-route", "CONFIG", false, "未配置有效模型路由");
        }
        return critical("model-route", "CONFIG", true,
                "主模型 " + route.get().getPrimaryModel());
    }

    private AgentProfileProbeCheckVO checkProviderPreset(AgentProfileDocument document, LlmAgentProfileDO profile) {
        String presetCode = StringUtils.hasText(document.getProviderRef())
                ? document.getProviderRef()
                : profile.getProviderPresetCode();
        if (!StringUtils.hasText(presetCode)) {
            return configCheck("provider-preset", false, "未指定 Provider 预设");
        }
        Optional<LlmProviderPresetDO> preset = providerPresetRepo.findByCode(presetCode);
        if (preset.isEmpty()) {
            return configCheck("provider-preset", false, "Provider 预设不存在: " + presetCode);
        }
        return configCheck("provider-preset", true,
                preset.get().getPresetName() + " · " + preset.get().getProviderType());
    }

    private List<AgentProfileProbeCheckVO> checkOutboundSecret(AgentProfileDocument document) {
        List<AgentProfileProbeCheckVO> checks = new ArrayList<>();
        OutboundAuthConfig outbound = document.getOutboundAuth();
        if (outbound == null || !StringUtils.hasText(outbound.getSecretRef())) {
            checks.add(configCheck("outbound-secret", true, "未配置出站 SecretRef（可选）"));
            return checks;
        }
        boolean resolved = secretResolver.resolve(outbound.getSecretRef()).filter(StringUtils::hasText).isPresent();
        checks.add(configCheck("outbound-secret", resolved,
                resolved ? "SecretRef 可解析: " + outbound.getSecretRef() : "SecretRef 无法解析: " + outbound.getSecretRef()));
        return checks;
    }

    private List<AgentProfileProbeCheckVO> probeMcpTools(AgentProfileDocument document) {
        List<AgentProfileProbeCheckVO> checks = new ArrayList<>();
        if (document.getTools() == null || document.getTools().isEmpty()) {
            checks.add(configCheck("mcp-tools", true, "未配置 MCP 工具"));
            return checks;
        }
        for (ToolDefinition tool : document.getTools()) {
            if (tool == null || !"mcp".equalsIgnoreCase(tool.getType())) {
                continue;
            }
            String serverRef = tool.getServerRef();
            String checkName = "mcp:" + (StringUtils.hasText(serverRef) ? serverRef : tool.getName());
            if (!StringUtils.hasText(serverRef)) {
                checks.add(configCheck(checkName, false, "MCP 工具缺少 serverRef"));
                continue;
            }
            Optional<LlmMcpServerDO> server = mcpServerRepo.findByCode(serverRef);
            if (server.isEmpty()) {
                checks.add(configCheck(checkName, false, "MCP Server 未注册: " + serverRef));
                continue;
            }
            try {
                String authToken = null;
                if (StringUtils.hasText(server.get().getAuthSecretRef())) {
                    authToken = secretResolver.resolve(server.get().getAuthSecretRef()).orElse(null);
                }
                int count = mcpToolAdapter.listTools(McpToolListRequest.builder()
                        .serverUrl(server.get().getBaseUrl())
                        .serverAuthToken(authToken)
                        .build()).size();
                checks.add(configCheck(checkName, true, serverRef + " 在线 · " + count + " 个工具"));
            } catch (Exception ex) {
                checks.add(configCheck(checkName, false, serverRef + " 不可达: " + ex.getMessage()));
            }
        }
        return checks;
    }

    private AgentProfileProbeCheckVO probeGatewayHealth(String baseUrl, String apiKey) {
        try {
            RestClient client = buildGatewayClient(baseUrl, apiKey);
            client.get().uri("/health/liveliness").retrieve().toBodilessEntity();
            return critical("gateway-health", "CONNECTIVITY", true, baseUrl + " 健康检查通过");
        } catch (Exception first) {
            try {
                RestClient client = buildGatewayClient(baseUrl, apiKey);
                client.get().uri("/health").retrieve().toBodilessEntity();
                return critical("gateway-health", "CONNECTIVITY", true, baseUrl + " /health 可达");
            } catch (Exception second) {
                return critical("gateway-health", "CONNECTIVITY", false,
                        baseUrl + " 不可达: " + first.getMessage());
            }
        }
    }

    private AgentProfileProbeCheckVO smokeInvoke(CachedPolicy policy) {
        String baseUrl = policy.getGatewayBaseUrl();
        String model = policy.getPrimaryModel();
        String apiKey = resolveGatewayApiKey(policy);
        try {
            RestClient client = buildGatewayClient(baseUrl, apiKey);
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", 8);
            ArrayNode messages = objectMapper.createArrayNode();
            messages.add(objectMapper.createObjectNode().put("role", "user").put("content", "ping"));
            body.set("messages", messages);
            String raw = client.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);
            boolean ok = raw != null && raw.contains("choices");
            return critical("smoke-invoke", "CONNECTIVITY", ok,
                    ok ? "冒烟调用成功 · model=" + model : "网关响应异常");
        } catch (Exception ex) {
            return critical("smoke-invoke", "CONNECTIVITY", false, "冒烟调用失败: " + ex.getMessage());
        }
    }

    private String resolveGatewayApiKey(CachedPolicy policy) {
        if (StringUtils.hasText(policy.getOutboundSecretRef())) {
            return secretResolver.resolve(policy.getOutboundSecretRef()).orElse(null);
        }
        return null;
    }

    private RestClient buildGatewayClient(String baseUrl, String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(PROBE_CONNECT_MS));
        factory.setReadTimeout(Duration.ofMillis(PROBE_READ_MS));
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl).requestFactory(factory);
        if (StringUtils.hasText(apiKey)) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }
        return builder.build();
    }

    private LlmAppDO resolveApp(LlmAiTaskDefDO task, String appKey) {
        if (StringUtils.hasText(appKey)) {
            return appRepo.findByAppKey(appKey)
                    .orElseThrow(() -> new BusinessException("APP_NOT_FOUND", "应用不存在: " + appKey));
        }
        return appRepo.findById(task.getAppId())
                .orElseThrow(() -> new BusinessException("APP_NOT_FOUND", "作业未关联有效应用"));
    }

    private LlmAiTaskDefDO requireTask(String taskCode) {
        return taskDefRepo.findByCode(taskCode)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在: " + taskCode));
    }

    private AgentProfileProbeCheckVO critical(String name, String category, boolean up, String message) {
        return AgentProfileProbeCheckVO.builder()
                .name(name)
                .category(category)
                .critical(true)
                .up(up)
                .message(message)
                .build();
    }

    private AgentProfileProbeCheckVO configCheck(String name, boolean up, String message) {
        return AgentProfileProbeCheckVO.builder()
                .name(name)
                .category("CONFIG")
                .critical(false)
                .up(up)
                .message(message)
                .build();
    }
}
