package cn.zest.www.zestllm.admin.model.vo;

import lombok.Data;

/**
 * ZestFlow 在线 Executor 对等节点（本地 VO，避免依赖未发布的 zestflow-common DTO）。
 */
@Data
public class PeerExecutorVO {
    private String executorId;
    private String host;
    private Integer port;
    private String appCode;
}
