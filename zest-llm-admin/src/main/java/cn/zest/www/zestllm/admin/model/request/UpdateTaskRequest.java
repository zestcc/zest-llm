package cn.zest.www.zestllm.admin.model.request;

import lombok.Data;

@Data
public class UpdateTaskRequest {
    private String name;
    private String description;
    private String status;
}
