package cn.zest.www.zestllm.common.api;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MethodRegistryRequest {
    private String appKey;
    private List<MethodRegistryItem> methods;
}
