package cn.zest.www.zestllm.admin.model.request;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class LearningSuggestCasesRequest {
    private String taskCode;
    private Instant since;
    private int limit = 20;
    private List<String> distillationSources;
}
