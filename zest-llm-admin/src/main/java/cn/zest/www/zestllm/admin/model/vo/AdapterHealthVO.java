package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdapterHealthVO {
    private String kind;
    private String configured;
    private String adapterId;
    private boolean up;
    private String message;
}
