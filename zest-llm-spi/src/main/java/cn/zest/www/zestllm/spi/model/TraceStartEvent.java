package cn.zest.www.zestllm.spi.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class TraceStartEvent {
    private String traceId;
    private String appKey;
    private String code;
    private String model;
    private Map<String, Object> metadata;
}
