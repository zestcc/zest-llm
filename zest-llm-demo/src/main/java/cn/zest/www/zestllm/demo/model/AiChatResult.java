package cn.zest.www.zestllm.demo.model;

import lombok.Data;

import java.util.List;

@Data
public class AiChatResult {

    private String answer;

    private Double confidence;

    private List<String> tags;

    private Boolean needManualReview;

    /** 平台 traceId，便于业务侧审计关联 */
    private String traceId;
}
