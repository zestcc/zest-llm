package cn.zest.www.zestllm.admin.model.dto;

import lombok.Data;

import java.util.Map;

@Data
public class PlaygroundRunCommand {
    private String appKey;
    private String code;
    private Map<String, Object> inputs;
    private String bizId;
}
