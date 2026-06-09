package cn.zest.www.zestllm.spi.schema;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ValidationResult {
    private boolean valid;
    private String message;
    private List<String> errors;
}
