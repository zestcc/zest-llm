package cn.zest.www.zestllm.admin.model.vo;

import lombok.Data;

@Data
public class AdminLoginVO {
    private String token;
    private long expiresIn;
    private String username;
    private String role;
}
