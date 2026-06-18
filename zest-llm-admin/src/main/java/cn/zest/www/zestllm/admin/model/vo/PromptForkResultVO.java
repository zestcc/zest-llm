package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PromptForkResultVO {

    private String version;
    private boolean published;
    private LocalDateTime publishedAt;
    private PromptVersionVO versionDetail;
}
