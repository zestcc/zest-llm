package cn.zest.www.zestllm.spi.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HealthStatus {
    private boolean up;
    private String message;
}
