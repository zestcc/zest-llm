package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McpServerVO {
    private Long id;
    private String serverCode;
    private String serverName;
    private String baseUrl;
    private String authSecretRef;
    private String configJson;
    private String status;
}
