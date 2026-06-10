package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiJobOverviewVO {
    private String code;
    private String name;
    private String appKey;
    private String status;
    private String publishedVersion;
    private String probeStatus;
    private LocalDateTime lastProbeAt;
    private long executionsLast7d;
    private long failedLast7d;
}
