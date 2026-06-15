package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdapterTroubleshootingItemVO {
    private String problem;
    private String solution;
}
