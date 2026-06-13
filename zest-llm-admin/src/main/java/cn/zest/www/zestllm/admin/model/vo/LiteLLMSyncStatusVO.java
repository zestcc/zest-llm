package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class LiteLLMSyncStatusVO {
    private boolean liteLLMReachable;
    private String liteLLMBaseUrl;
    private int total;
    private int synced;
    private int failed;
    private int pending;
    private List<ModelSyncItem> models;

    @Data
    @Builder
    public static class ModelSyncItem {
        private String modelName;
        private String upstreamModel;
        private String syncStatus;
        private LocalDateTime lastSyncAt;
    }
}
