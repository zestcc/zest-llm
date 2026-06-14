package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.vo.IntegrationSetupChecklistVO;
import cn.zest.www.zestllm.admin.model.vo.IntegrationSetupChecklistVO.IntegrationSetupStepVO;
import cn.zest.www.zestllm.admin.plugin.AdapterEnablementChecker;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmGatewayModelRepo;
import cn.zest.www.zestllm.admin.repo.LlmMcpServerRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IntegrationSetupGuideService {

    private final AdapterHealthService adapterHealthService;
    private final AdapterEnablementChecker enablementChecker;
    private final LlmGatewayModelRepo gatewayModelRepo;
    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmMcpServerRepo mcpServerRepo;

    public IntegrationSetupChecklistVO buildChecklist() {
        List<IntegrationSetupStepVO> steps = new ArrayList<>();

        boolean gatewayUp = adapterHealthService.gatewayHealth().isUp();
        steps.add(step("gateway_health", "模型网关连通", "LiteLLM / Model Gateway 健康探测通过",
                gatewayUp ? "done" : "failed", true, "/adapters",
                "启动 LiteLLM 并配置 zest.llm.litellm.base-url"));

        long modelCount = gatewayModelRepo.findAllActive().size();
        steps.add(step("gateway_models", "注册 Gateway 模型", "至少 1 个 ACTIVE Gateway 模型",
                modelCount > 0 ? "done" : "pending", true, "/integration",
                "集成概览 → Import 模型 → 触发 LiteLLM 同步"));

        boolean ragConfigured = !"noop".equals(enablementChecker.resolveActivePluginId("knowledge-retrieval"));
        steps.add(step("knowledge_plugin", "知识检索插件（按需）", "RAG 场景需选择 http-knowledge / ragflow 等",
                ragConfigured ? "done" : "warning", false, "/plugin-catalog",
                "插件中心 → knowledge-retrieval → 跟随集成步骤"));

        long taskCount = taskDefRepo.findAll().size();
        steps.add(step("ai_task", "创建 AI 作业", "至少 1 个 Task 定义",
                taskCount > 0 ? "done" : "pending", true, "/tasks",
                "或使用场景模板一键生成"));

        long mcpCount = mcpServerRepo.findAllActive().size();
        steps.add(step("mcp_optional", "MCP 工具（按需）", "Agent+MCP 场景注册 MCP Server",
                mcpCount > 0 ? "done" : "warning", false, "/agent-config",
                "智能体配置 → MCP Server"));

        steps.add(step("plugin_catalog", "浏览插件中心", "了解各 SPI 插件与分步集成指南",
                "done", false, "/plugin-catalog",
                "每个插件详情页含 CONFIG / VERIFY / NAVIGATE 步骤"));

        long completed = steps.stream().filter(item -> "done".equals(item.getStatus())).count();
        boolean ready = steps.stream()
                .filter(IntegrationSetupStepVO::isRequired)
                .allMatch(item -> "done".equals(item.getStatus()));

        return IntegrationSetupChecklistVO.builder()
                .totalSteps(steps.size())
                .completedSteps((int) completed)
                .progressPercent(steps.isEmpty() ? 0 : (int) (completed * 100 / steps.size()))
                .readyForProduction(ready)
                .steps(steps)
                .build();
    }

    private IntegrationSetupStepVO step(String stepId, String title, String description, String status,
                                        boolean required, String navigateTo, String hint) {
        return IntegrationSetupStepVO.builder()
                .stepId(stepId)
                .title(title)
                .description(description)
                .status(status)
                .required(required)
                .navigateTo(navigateTo)
                .hint(hint)
                .build();
    }
}
