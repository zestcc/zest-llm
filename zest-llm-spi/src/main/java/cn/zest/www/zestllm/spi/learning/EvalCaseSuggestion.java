package cn.zest.www.zestllm.spi.learning;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvalCaseSuggestion {

    private String traceId;
    private String suggestedInput;
    private String suggestedExpected;
    private String reason;
    private String source;
}
