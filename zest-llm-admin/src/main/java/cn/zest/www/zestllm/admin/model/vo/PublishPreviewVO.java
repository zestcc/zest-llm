package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PublishPreviewVO {
    private String taskCode;
    private String version;
    private boolean publishAllowed;
    private boolean probePassed;
    private double passRate;
    private int totalCases;
    private int passedCases;
    private String message;
    private List<String> failedCaseCodes;
    private boolean learningLoopEnabled;
}
