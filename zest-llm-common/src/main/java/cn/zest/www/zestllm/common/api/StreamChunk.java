package cn.zest.www.zestllm.common.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StreamChunk {
    private String traceId;
    private String type;
    private String delta;
    private boolean done;
}
