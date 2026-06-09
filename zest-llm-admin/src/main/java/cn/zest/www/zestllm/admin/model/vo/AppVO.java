package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AppVO {
    private Long id;
    private String appKey;
    private String appName;
    private String status;
    private LocalDateTime createdAt;
}
