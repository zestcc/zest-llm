package cn.zest.www.zestllm.admin.model.request;

import lombok.Data;

import java.util.List;

@Data
public class IntegrationImportProviderPresetsRequest {
    private List<CreateProviderPresetRequest> items;
}
