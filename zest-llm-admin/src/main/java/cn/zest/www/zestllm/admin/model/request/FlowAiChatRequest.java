package cn.zest.www.zestllm.admin.model.request;

import lombok.Data;

@Data
public class FlowAiChatRequest {
    private String question;
    private String bearerToken;
}
