package cn.zest.www.zestllm.spi.knowledge;

import cn.zest.www.zestllm.spi.profile.KnowledgeRefConfig;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeRetrieveRequest {

    private String traceId;
    private String tenantCode;
    private String appKey;
    private String taskCode;
    private String query;
    private KnowledgeRefConfig knowledge;
}
