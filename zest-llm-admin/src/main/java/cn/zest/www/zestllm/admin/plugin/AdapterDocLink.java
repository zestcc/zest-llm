package cn.zest.www.zestllm.admin.plugin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdapterDocLink {

    private String label;
    private String url;
}
