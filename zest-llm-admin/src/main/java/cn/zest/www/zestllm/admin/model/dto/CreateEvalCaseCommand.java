package cn.zest.www.zestllm.admin.model.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CreateEvalCaseCommand {
    private String caseCode;
    private Map<String, Object> inputs;
    private Map<String, Object> expected;
}
