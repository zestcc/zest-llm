package cn.zest.www.zestllm.admin.model.dto;

import cn.zest.www.zestllm.common.api.InvokeRequest;
import lombok.Data;

@Data
public class InvokeCommand {
    private String bearerToken;
    private InvokeRequest request;
    /** Admin Playground / Eval 内部调用，跳过 Bearer 鉴权 */
    private boolean adminBypass;
}
