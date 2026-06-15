package cn.zest.www.zestllm.admin.plugin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdapterTroubleshootingItem {

    private String problem;
    private String solution;
}
