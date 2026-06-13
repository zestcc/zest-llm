package cn.zest.www.zestllm.admin.model.request;

import lombok.Data;

import java.util.List;

@Data
public class IntegrationImportProviderPresetsRequest {
    /** 为 true 时仅预览将创建/更新/跳过的数量，不写入数据库（参考 Dify/Portkey 导入预览） */
    private boolean dryRun;
    private List<CreateProviderPresetRequest> items;
}
