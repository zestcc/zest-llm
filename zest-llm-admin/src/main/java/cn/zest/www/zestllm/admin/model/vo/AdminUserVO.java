package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserVO {
    private Long id;
    private String username;
    private String displayName;
    private String role;
    private String status;
    private LocalDateTime createdAt;
}
