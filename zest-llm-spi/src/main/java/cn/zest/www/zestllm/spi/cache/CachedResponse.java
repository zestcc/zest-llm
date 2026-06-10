package cn.zest.www.zestllm.spi.cache;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CachedResponse {
    private String model;
    private String promptVersion;
    private Map<String, Object> output;
    private String status;
}
