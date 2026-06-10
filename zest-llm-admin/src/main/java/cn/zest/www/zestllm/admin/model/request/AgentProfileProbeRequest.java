package cn.zest.www.zestllm.admin.model.request;

import lombok.Data;

@Data
public class AgentProfileProbeRequest {
    /** 可选：指定应用，默认使用作业所属 App */
    private String appKey;
    /** 是否执行网关冒烟调用（会产生少量 token 消耗） */
    private boolean smokeTest;
}
