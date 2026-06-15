package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdapterConfigRefVO {
    private String key;
    private String description;
    private boolean required;
    private String example;
    private String envVar;
}
