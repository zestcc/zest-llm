package cn.zest.www.zestllm.admin.model.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateAdminUserRequest {
    private String displayName;
    private String password;
    @Pattern(regexp = "ADMIN|OPERATOR")
    private String role;
    @Pattern(regexp = "ACTIVE|DISABLED")
    private String status;
}
