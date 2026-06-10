package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProviderPresetVO {
    private Long id;
    private String presetCode;
    private String presetName;
    private String providerType;
    private String authMode;
    private String configJson;
    private Integer sortOrder;
    private String status;
    private String tenantCode;
    private LocalDateTime updatedAt;
}
