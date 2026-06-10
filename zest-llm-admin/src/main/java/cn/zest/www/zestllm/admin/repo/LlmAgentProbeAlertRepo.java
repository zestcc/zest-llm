package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmAgentProbeAlertMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProbeAlertDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    public void updateById(LlmAgentProbeAlertDO entity) {
        mapper.updateById(entity);
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
        return page(taskCode, 1, limit).getRecords();
    }

    public Optional<LlmAgentProbeAlertDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    public Page<LlmAgentProbeAlertDO> page(String taskCode, int pageNum, int pageSize) {
        Page<LlmAgentProbeAlertDO> pager = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<LlmAgentProbeAlertDO> query = new LambdaQueryWrapper<LlmAgentProbeAlertDO>()
                .orderByDesc(LlmAgentProbeAlertDO::getCreatedAt);
        if (taskCode != null && !taskCode.isBlank()) {
            query.eq(LlmAgentProbeAlertDO::getTaskCode, taskCode);
        }
        mapper.selectPage(pager, query);
        return pager;
    }
}
