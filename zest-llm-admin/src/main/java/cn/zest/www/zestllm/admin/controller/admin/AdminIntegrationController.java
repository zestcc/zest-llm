package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.IntegrationImportAgentProfilesRequest;
import cn.zest.www.zestllm.admin.model.request.IntegrationImportGatewayModelsRequest;
import cn.zest.www.zestllm.admin.model.request.IntegrationImportProviderPresetsRequest;
import cn.zest.www.zestllm.admin.model.vo.IntegrationImportResultVO;
import cn.zest.www.zestllm.admin.model.vo.IntegrationOverviewVO;
import cn.zest.www.zestllm.admin.model.vo.IntegrationWebhookDeliveryVO;
import cn.zest.www.zestllm.admin.model.vo.LiteLLMSyncResultVO;
import cn.zest.www.zestllm.admin.model.vo.LiteLLMSyncStatusVO;
import cn.zest.www.zestllm.admin.service.IntegrationImportService;
import cn.zest.www.zestllm.admin.service.IntegrationOverviewService;
import cn.zest.www.zestllm.admin.service.IntegrationWebhookDeliveryService;
import cn.zest.www.zestllm.admin.service.LiteLLMSyncService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/integration")
@RequiredArgsConstructor
public class AdminIntegrationController {

    private final IntegrationImportService integrationImportService;
    private final LiteLLMSyncService liteLLMSyncService;
    private final IntegrationOverviewService integrationOverviewService;
    private final IntegrationWebhookDeliveryService webhookDeliveryService;

    @GetMapping("/overview")
    public Result<IntegrationOverviewVO> overview() {
        return Result.success(integrationOverviewService.overview());
    }

    @PostMapping("/import/provider-presets")
    public Result<IntegrationImportResultVO> importProviderPresets(
            @RequestBody IntegrationImportProviderPresetsRequest request) {
        return Result.success(integrationImportService.importProviderPresets(request));
    }

    @PostMapping("/import/agent-profiles")
    public Result<IntegrationImportResultVO> importAgentProfiles(
            @RequestBody IntegrationImportAgentProfilesRequest request) {
        return Result.success(integrationImportService.importAgentProfiles(request));
    }

    @PostMapping("/import/gateway-models")
    public Result<IntegrationImportResultVO> importGatewayModels(
            @RequestBody IntegrationImportGatewayModelsRequest request) {
        return Result.success(integrationImportService.importGatewayModels(request));
    }

    @GetMapping("/sync-litellm/status")
    public Result<LiteLLMSyncStatusVO> syncLiteLLMStatus() {
        return Result.success(liteLLMSyncService.getSyncStatus());
    }

    @PostMapping("/sync-litellm")
    public Result<LiteLLMSyncResultVO> syncLiteLLM() {
        return Result.success(liteLLMSyncService.syncAll());
    }

    @GetMapping("/webhook/deliveries")
    public Result<Page<IntegrationWebhookDeliveryVO>> listWebhookDeliveries(
            @RequestParam(required = false) String taskCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(webhookDeliveryService.page(taskCode, page, size));
    }

    @PostMapping("/webhook/deliveries/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IntegrationWebhookDeliveryVO> retryWebhookDelivery(@PathVariable Long id) {
        return Result.success(webhookDeliveryService.retry(id));
    }
}
