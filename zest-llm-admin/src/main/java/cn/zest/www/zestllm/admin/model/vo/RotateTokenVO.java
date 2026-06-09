package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RotateTokenVO {
    private String appKey;
    private String rawToken;
}
