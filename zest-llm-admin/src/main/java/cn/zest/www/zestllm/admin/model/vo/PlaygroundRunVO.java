package cn.zest.www.zestllm.admin.model.vo;

import cn.zest.www.zestllm.common.api.InvokeMetrics;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class PlaygroundRunVO {
    private String traceId;
    private String status;
    private String code;
    private String promptVersion;
    private String model;
    private Map<String, Object> output;
    private InvokeMetrics metrics;
    private Boolean cacheHit;
    private String errorCode;
    private String errorMessage;
}
