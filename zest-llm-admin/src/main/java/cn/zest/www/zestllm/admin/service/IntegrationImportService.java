package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.request.CreateGatewayModelRequest;
import cn.zest.www.zestllm.admin.model.request.CreateProviderPresetRequest;
import cn.zest.www.zestllm.admin.model.request.ImportAgentProfileRequest;
import cn.zest.www.zestllm.admin.model.request.IntegrationImportAgentProfilesRequest;
import cn.zest.www.zestllm.admin.model.request.IntegrationImportGatewayModelsRequest;
import cn.zest.www.zestllm.admin.model.request.IntegrationImportProviderPresetsRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateProviderPresetRequest;
import cn.zest.www.zestllm.admin.model.vo.IntegrationImportResultVO;
import cn.zest.www.zestllm.admin.repo.LlmAgentProfileRepo;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmGatewayModelRepo;
import cn.zest.www.zestllm.admin.repo.LlmProviderPresetRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IntegrationImportService {

    private final ProviderPresetManageService providerPresetManageService;
    private final LlmProviderPresetRepo providerPresetRepo;
    private final AgentProfileManageService agentProfileManageService;
    private final ModelRegistryManageService modelRegistryManageService;
    private final LlmGatewayModelRepo gatewayModelRepo;
    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmAgentProfileRepo agentProfileRepo;
    private final AgentProfileResolver agentProfileResolver;

    @Transactional(rollbackFor = Exception.class)
    public IntegrationImportResultVO importProviderPresets(IntegrationImportProviderPresetsRequest request) {
        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        if (request.getItems() == null) {
            return emptyResult(request.isDryRun(), errors);
        }
        for (CreateProviderPresetRequest item : request.getItems()) {
            try {
                if (providerPresetRepo.findByCode(item.getPresetCode()).isPresent()) {
                    if (!request.isDryRun()) {
                        UpdateProviderPresetRequest update = new UpdateProviderPresetRequest();
                        update.setPresetName(item.getPresetName());
                        update.setProviderType(item.getProviderType());
                        update.setAuthMode(item.getAuthMode());
                        update.setConfigJson(item.getConfigJson());
                        update.setSortOrder(item.getSortOrder());
                        providerPresetManageService.update(item.getPresetCode(), update);
                    }
                    updated++;
                } else {
                    if (!request.isDryRun()) {
                        providerPresetManageService.create(item);
                    }
                    created++;
                }
            } catch (BusinessException ex) {
                errors.add(item.getPresetCode() + ": " + ex.getMessage());
                skipped++;
            } catch (Exception ex) {
                errors.add(item.getPresetCode() + ": " + ex.getMessage());
                skipped++;
            }
        }
        return IntegrationImportResultVO.builder()
                .dryRun(request.isDryRun())
                .created(created).updated(updated).skipped(skipped).errors(errors).build();
    }

    @Transactional(rollbackFor = Exception.class)
    public IntegrationImportResultVO importAgentProfiles(IntegrationImportAgentProfilesRequest request) {
        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        if (request.getItems() == null) {
            return emptyResult(request.isDryRun(), errors);
        }
        for (ImportAgentProfileRequest item : request.getItems()) {
            try {
                if (request.isDryRun()) {
                    if (previewAgentProfileWouldUpdate(item)) {
                        updated++;
                    } else {
                        created++;
                    }
                } else {
                    agentProfileManageService.importProfile(item);
                    created++;
                }
            } catch (BusinessException ex) {
                if ("PROFILE_EXISTS".equals(ex.getErrorCode())) {
                    skipped++;
                } else {
                    errors.add(item.getTaskCode() + ": " + ex.getMessage());
                    skipped++;
                }
            } catch (Exception ex) {
                errors.add(item.getTaskCode() + ": " + ex.getMessage());
                skipped++;
            }
        }
        return IntegrationImportResultVO.builder()
                .dryRun(request.isDryRun())
                .created(created).updated(updated).skipped(skipped).errors(errors).build();
    }

    @Transactional(rollbackFor = Exception.class)
    public IntegrationImportResultVO importGatewayModels(IntegrationImportGatewayModelsRequest request) {
        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        if (request.getItems() == null) {
            return emptyResult(request.isDryRun(), errors);
        }
        for (CreateGatewayModelRequest item : request.getItems()) {
            try {
                boolean existed = gatewayModelRepo.findByModelName(item.getModelName()).isPresent();
                if (!request.isDryRun()) {
                    modelRegistryManageService.upsertForImport(item);
                }
                if (existed) {
                    updated++;
                } else {
                    created++;
                }
            } catch (BusinessException ex) {
                errors.add(item.getModelName() + ": " + ex.getMessage());
                skipped++;
            } catch (Exception ex) {
                errors.add(item.getModelName() + ": " + ex.getMessage());
                skipped++;
            }
        }
        return IntegrationImportResultVO.builder()
                .dryRun(request.isDryRun())
                .created(created).updated(updated).skipped(skipped).errors(errors).build();
    }

    /** @return true when dry-run import would conflict with an existing profile version */
    private boolean previewAgentProfileWouldUpdate(ImportAgentProfileRequest item) {
        if (!StringUtils.hasText(item.getTaskCode())) {
            throw new BusinessException("TASK_REQUIRED", "taskCode is required");
        }
        agentProfileResolver.parseProfile(item.getProfileJson(), null);
        Optional<LlmAiTaskDefDO> task = taskDefRepo.findByCode(item.getTaskCode());
        String version = StringUtils.hasText(item.getVersion()) ? item.getVersion() : "v-preview";
        return task.isPresent()
                && agentProfileRepo.findByTaskIdAndVersion(task.get().getId(), version).isPresent();
    }

    private IntegrationImportResultVO emptyResult(boolean dryRun, List<String> errors) {
        return IntegrationImportResultVO.builder()
                .dryRun(dryRun)
                .created(0).updated(0).skipped(0).errors(errors).build();
    }
}
