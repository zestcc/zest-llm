package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IntegrationOverviewVO {
    private GatewayModelSummary gatewayModels;
    private int secretRefCount;
    private boolean liteLLMReachable;
    private int adaptersUp;
    private int adaptersDown;
    private List<AdapterHealthVO> adapterIssues;

    @Data
    @Builder
    public static class GatewayModelSummary {
        private int total;
        private int synced;
        private int failed;
        private int pending;
    }
}
