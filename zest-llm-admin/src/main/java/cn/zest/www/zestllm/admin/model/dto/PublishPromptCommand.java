package cn.zest.www.zestllm.admin.model.dto;

import lombok.Data;

@Data
public class PublishPromptCommand {
    private String taskCode;
    private String version;
    private String operator;
}
