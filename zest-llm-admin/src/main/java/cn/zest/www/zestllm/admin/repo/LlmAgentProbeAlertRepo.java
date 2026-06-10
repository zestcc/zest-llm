package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmAgentProbeAlertMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProbeAlertDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmAgentProbeAlertRepo {

    private final LlmAgentProbeAlertMapper mapper;

    public void insert(LlmAgentProbeAlertDO entity) {
        mapper.insert(entity);
    }

    public Optional<LlmAgentProbeAlertDO> findRecentAlert(Long taskId, String overallStatus, LocalDateTime since) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmAgentProbeAlertDO>()
                .eq(LlmAgentProbeAlertDO::getTaskId, taskId)
                .eq(LlmAgentProbeAlertDO::getOverallStatus, overallStatus)
                .ge(LlmAgentProbeAlertDO::getCreatedAt, since)
                .orderByDesc(LlmAgentProbeAlertDO::getCreatedAt)
                .last("LIMIT 1")));
    }

    public List<LlmAgentProbeAlertDO> listRecent(String taskCode, int limit) {
        LambdaQueryWrapper<LlmAgentProbeAlertDO> query = new LambdaQueryWrapper<LlmAgentProbeAlertDO>()
                .orderByDesc(LlmAgentProbeAlertDO::getCreatedAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100)));
        if (taskCode != null && !taskCode.isBlank()) {
            query.eq(LlmAgentProbeAlertDO::getTaskCode, taskCode);
        }
        return mapper.selectList(query);
    }
}
