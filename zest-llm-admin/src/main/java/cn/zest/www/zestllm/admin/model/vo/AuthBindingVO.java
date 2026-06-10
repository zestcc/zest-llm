package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuthBindingVO {
    private Long id;
    private String scopeType;
    private Long scopeId;
    private String appKey;
    private String taskCode;
    private String inboundMode;
    private String inboundConfigJson;
    private String outboundMode;
    private String outboundConfigJson;
    private String status;
    private LocalDateTime updatedAt;
}
