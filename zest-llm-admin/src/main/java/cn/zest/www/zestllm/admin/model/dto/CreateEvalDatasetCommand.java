package cn.zest.www.zestllm.admin.model.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CreateEvalDatasetCommand {
    private String datasetCode;
    private String datasetName;
    private String appKey;
    private String taskCode;
}
