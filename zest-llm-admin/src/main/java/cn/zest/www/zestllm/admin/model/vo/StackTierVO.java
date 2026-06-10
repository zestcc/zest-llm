package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class StackTierVO {
    private String id;
    private String name;
    private String description;
    private List<String> components;
    private Map<String, String> adapterDefaults;
    private String expectedQps;
    private String composeHint;
}
