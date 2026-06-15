package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdapterDocLinkVO {
    private String label;
    private String url;
}
