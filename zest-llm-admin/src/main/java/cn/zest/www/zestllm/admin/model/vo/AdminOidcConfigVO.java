package cn.zest.www.zestllm.admin.model.vo;

import lombok.Data;

@Data
public class AdminOidcConfigVO {
    private boolean enabled;
    private String clientId;
    private String issuer;
}
