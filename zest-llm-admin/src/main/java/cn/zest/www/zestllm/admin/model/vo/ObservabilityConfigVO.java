package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ObservabilityConfigVO {
    private String adapterId;
    private boolean langfuseEnabled;
    private String langfuseUiBaseUrl;
    private String tracePathTemplate;
}
