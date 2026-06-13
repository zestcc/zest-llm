package cn.zest.www.zestllm.admin.model.request;

import lombok.Data;

import java.util.List;

@Data
public class IntegrationImportAgentProfilesRequest {
    private List<ImportAgentProfileRequest> items;
}
