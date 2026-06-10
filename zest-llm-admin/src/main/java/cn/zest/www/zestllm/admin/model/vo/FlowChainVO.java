package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FlowChainVO {
    private Long id;
    private String chainCode;
    private String chainName;
    private Integer version;
    private String lifecycle;
    private String chainData;
    private String status;
    private LocalDateTime updatedAt;
}
