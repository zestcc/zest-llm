package cn.zest.www.zestllm.common.api;

import lombok.Data;

import java.util.List;

@Data
public class MethodRegistryItem {
    private String code;
    private String methodSignature;
    private List<String> inputFields;
    private String outputClass;
}
