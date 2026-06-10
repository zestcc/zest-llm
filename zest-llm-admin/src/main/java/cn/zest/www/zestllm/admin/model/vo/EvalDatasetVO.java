package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvalDatasetVO {
    private Long id;
    private String datasetCode;
    private String datasetName;
    private String appKey;
    private String taskCode;
    private String status;
}
