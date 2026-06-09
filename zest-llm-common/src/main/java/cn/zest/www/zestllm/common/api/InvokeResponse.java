package cn.zest.www.zestllm.common.api;

import lombok.Data;

import java.util.Map;

@Data
public class InvokeResponse {
    private String traceId;
    private String status;
    private String code;
    private String promptVersion;
    private String model;
    private Map<String, Object> output;
    private InvokeMetrics metrics;
    private String errorCode;
    private String errorMessage;

    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }
}
