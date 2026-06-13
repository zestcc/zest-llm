package cn.zest.www.zestllm.admin.model.vo;

import lombok.Data;

@Data
public class AdminOidcAuthorizeVO {

    private String authorizationUrl;
    private String state;
}
