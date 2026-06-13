package cn.zest.www.zestllm.admin.model.request;

import lombok.Data;

import java.util.List;

@Data
public class IntegrationImportAgentProfilesRequest {
    private boolean dryRun;
    private List<ImportAgentProfileRequest> items;
}
