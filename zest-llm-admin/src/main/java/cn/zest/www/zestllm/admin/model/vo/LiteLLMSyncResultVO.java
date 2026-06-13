package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LiteLLMSyncResultVO {
    private int total;
    private int synced;
    private int failed;
    private String message;
}
