package cn.zest.www.zestllm.common.api;

import lombok.Data;

@Data
public class InvokeOptions {
    private Long timeoutMs;
    private Integer retry;
}
