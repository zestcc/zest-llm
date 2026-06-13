package cn.zest.www.zestllm.admin.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOidcCallbackRequest {

    private String code;
    private String state;
}
