package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class EvalCaseVO {
    private Long id;
    private String caseCode;
    private Map<String, Object> inputs;
    private Map<String, Object> expected;
    private String status;
    private LocalDateTime createdAt;
}
