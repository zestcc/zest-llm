package cn.zest.www.zestllm.admin.model.request;

import lombok.Data;

@Data
public class UpdateAppRequest {
    private String appName;
    private String status;
}
