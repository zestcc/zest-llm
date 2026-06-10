package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmMcpServerDO;
import cn.zest.www.zestllm.admin.model.request.CreateMcpServerRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateMcpServerRequest;
import cn.zest.www.zestllm.admin.model.vo.McpServerVO;
import cn.zest.www.zestllm.admin.repo.LlmMcpServerRepo;
import cn.zest.www.zestllm.spi.secret.SecretResolver;
import cn.zest.www.zestllm.spi.tool.McpToolAdapter;
import cn.zest.www.zestllm.spi.tool.McpToolDescriptor;
import cn.zest.www.zestllm.spi.tool.McpToolListRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class McpServerManageService {

    private final LlmMcpServerRepo mcpServerRepo;
    private final AuditService auditService;
    private final McpToolAdapter mcpToolAdapter;
    private final SecretResolver secretResolver;

    public List<McpServerVO> list() {
        return mcpServerRepo.findAllActive().stream().map(this::toVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public McpServerVO create(CreateMcpServerRequest request) {
        if (mcpServerRepo.findByCodeAnyStatus(request.getServerCode()).isPresent()) {
            throw new BusinessException("MCP_SERVER_EXISTS", "MCP Server 已存在: " + request.getServerCode());
        }
        LlmMcpServerDO server = new LlmMcpServerDO();
        server.setServerCode(request.getServerCode());
        server.setServerName(request.getServerName());
        server.setBaseUrl(request.getBaseUrl());
        server.setAuthSecretRef(request.getAuthSecretRef());
        server.setConfigJson(StringUtils.hasText(request.getConfigJson()) ? request.getConfigJson() : "{}");
        server.setStatus("ACTIVE");
        server.setCreatedAt(LocalDateTime.now());
        server.setUpdatedAt(LocalDateTime.now());
        mcpServerRepo.insert(server);
        auditService.log("CREATE", "MCP_SERVER", request.getServerCode(), Map.of());
        return toVO(server);
    }

    @Transactional(rollbackFor = Exception.class)
    public McpServerVO update(String serverCode, UpdateMcpServerRequest request) {
        LlmMcpServerDO server = mcpServerRepo.findByCodeAnyStatus(serverCode)
                .orElseThrow(() -> new BusinessException("MCP_SERVER_NOT_FOUND", "MCP Server 不存在: " + serverCode));
        if (StringUtils.hasText(request.getServerName())) {
            server.setServerName(request.getServerName());
        }
        if (StringUtils.hasText(request.getBaseUrl())) {
            server.setBaseUrl(request.getBaseUrl());
        }
        if (request.getAuthSecretRef() != null) {
            server.setAuthSecretRef(request.getAuthSecretRef());
        }
        if (request.getConfigJson() != null) {
            server.setConfigJson(request.getConfigJson());
        }
        if (StringUtils.hasText(request.getStatus())) {
            server.setStatus(request.getStatus());
        }
        server.setUpdatedAt(LocalDateTime.now());
        mcpServerRepo.update(server);
        auditService.log("UPDATE", "MCP_SERVER", serverCode, Map.of());
        return toVO(server);
    }

    public List<McpToolDescriptor> listRemoteTools(String serverCode) {
        LlmMcpServerDO server = mcpServerRepo.findByCodeAnyStatus(serverCode)
                .orElseThrow(() -> new BusinessException("MCP_SERVER_NOT_FOUND", "MCP Server 不存在: " + serverCode));
        String authToken = null;
        if (StringUtils.hasText(server.getAuthSecretRef())) {
            authToken = secretResolver.resolve(server.getAuthSecretRef()).orElse(null);
        }
        return mcpToolAdapter.listTools(McpToolListRequest.builder()
                .serverUrl(server.getBaseUrl())
                .serverAuthToken(authToken)
                .build());
    }

    private McpServerVO toVO(LlmMcpServerDO server) {
        return McpServerVO.builder()
                .id(server.getId())
                .serverCode(server.getServerCode())
                .serverName(server.getServerName())
                .baseUrl(server.getBaseUrl())
                .authSecretRef(server.getAuthSecretRef())
                .configJson(server.getConfigJson())
                .status(server.getStatus())
                .build();
    }
}
