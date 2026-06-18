package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForkPromptVersionRequest {

    @NotBlank
    private String baseVersion;

    /** 为空时由服务端按 v1/v2… 自动递增 */
    private String version;

    @NotBlank
    private String templateBody;

    private String outputSchema;

    /** 默认 true：基于旧版编辑后创建新版本并发布 */
    private boolean publish = true;
}
