package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PromptVersionVO {
    private Long id;
    private String version;
    private String status;
    private String templateBody;
    private String outputSchema;
    private LocalDateTime publishedAt;
    private String createdBy;
    private LocalDateTime createdAt;
}
