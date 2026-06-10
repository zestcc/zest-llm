package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmExecutionMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmExecutionDO;
import cn.zest.www.zestllm.admin.model.vo.DailyCostVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmExecutionRepo {

    private final LlmExecutionMapper mapper;

    public void insert(LlmExecutionDO entity) {
        mapper.insert(entity);
    }

    public void updateByTraceId(LlmExecutionDO entity) {
        mapper.update(entity, new LambdaQueryWrapper<LlmExecutionDO>()
                .eq(LlmExecutionDO::getTraceId, entity.getTraceId()));
    }

    public Optional<LlmExecutionDO> findByTraceId(String traceId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmExecutionDO>()
                .eq(LlmExecutionDO::getTraceId, traceId)));
    }

    public Page<LlmExecutionDO> page(int pageNum, int pageSize, String taskCode, String status) {
        LambdaQueryWrapper<LlmExecutionDO> wrapper = new LambdaQueryWrapper<LlmExecutionDO>()
                .orderByDesc(LlmExecutionDO::getCreatedAt);
        if (taskCode != null && !taskCode.isBlank()) {
            wrapper.eq(LlmExecutionDO::getTaskCode, taskCode);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(LlmExecutionDO::getStatus, status);
        }
        return mapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<LlmExecutionDO> findRecentByTaskAndStatus(String taskCode, String status,
                                                          java.time.LocalDateTime since, int limit) {
        LambdaQueryWrapper<LlmExecutionDO> wrapper = new LambdaQueryWrapper<LlmExecutionDO>()
                .orderByDesc(LlmExecutionDO::getCreatedAt)
                .last("LIMIT " + Math.max(1, limit));
        if (taskCode != null && !taskCode.isBlank()) {
            wrapper.eq(LlmExecutionDO::getTaskCode, taskCode);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(LlmExecutionDO::getStatus, status);
        }
        if (since != null) {
            wrapper.ge(LlmExecutionDO::getCreatedAt, since);
        }
        return mapper.selectList(wrapper);
    }

    public long countAll() {
        return mapper.countAll();
    }

    public long countSuccess() {
        return mapper.countSuccess();
    }

    public long countFailed() {
        return mapper.countFailed();
    }

    public BigDecimal sumTotalCost() {
        return mapper.sumTotalCost();
    }

    public long countToday() {
        return mapper.countToday();
    }

    public List<DailyCostVO> dailyCostSince(java.time.LocalDateTime startAt) {
        return mapper.dailyCostSince(startAt);
    }

    public long countByTaskCodeSince(String taskCode, java.time.LocalDateTime since) {
        if (taskCode == null || taskCode.isBlank()) {
            return 0;
        }
        return mapper.countByTaskCodeSince(taskCode, since);
    }

    public long countByTaskCodeAndStatusSince(String taskCode, String status, java.time.LocalDateTime since) {
        if (taskCode == null || taskCode.isBlank()) {
            return 0;
        }
        return mapper.countByTaskCodeAndStatusSince(taskCode, status, since);
    }

    public long countByAppIdSince(Long appId, java.time.LocalDateTime since) {
        if (appId == null) {
            return 0;
        }
        return mapper.countByAppIdSince(appId, since);
    }

    public long countByAppIdAndStatusSince(Long appId, String status, java.time.LocalDateTime since) {
        if (appId == null) {
            return 0;
        }
        return mapper.countByAppIdAndStatusSince(appId, status, since);
    }
}
