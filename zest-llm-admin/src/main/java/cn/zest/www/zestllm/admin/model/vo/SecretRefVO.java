package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SecretRefVO {
    private Long id;
    private String secretCode;
    private String secretName;
    private String secretType;
    /** Masked preview, never full secret in list */
    private String secretPreview;
    private String envKey;
    private String scopeType;
    private Long scopeId;
    private String status;
    private LocalDateTime updatedAt;
}
