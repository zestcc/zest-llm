package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MethodRegistryVO {
    private Long id;
    private String code;
    private String appKey;
    private String methodSignature;
    private String inputFields;
    private String outputClass;
    private LocalDateTime registeredAt;
}
