package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateAdminUserRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private String displayName;
    @Pattern(regexp = "ADMIN|OPERATOR")
    private String role;
}
