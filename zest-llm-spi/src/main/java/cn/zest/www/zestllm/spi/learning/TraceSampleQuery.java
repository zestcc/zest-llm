package cn.zest.www.zestllm.spi.learning;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class TraceSampleQuery {

    private String taskCode;
    private Instant since;
    private int limit;
    private List<String> distillationSources;
}
