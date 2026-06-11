package cn.zest.www.zestllm.admin.model.dto;

import lombok.Data;

import java.util.Map;

@Data
public class UpdateEvalCaseCommand {
    private Map<String, Object> inputs;
    private Map<String, Object> expected;
}
