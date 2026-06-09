package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskVO {
    private Long id;
    private String appKey;
    private String code;
    private String name;
    private String description;
    private String status;
    private LocalDateTime createdAt;
}
