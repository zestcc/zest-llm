package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmAgentProfileProbeMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileProbeDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmAgentProfileProbeRepo {

    private final LlmAgentProfileProbeMapper mapper;

    public void insert(LlmAgentProfileProbeDO entity) {
        mapper.insert(entity);
    }

    public Optional<LlmAgentProfileProbeDO> findLatestByTaskId(Long taskId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmAgentProfileProbeDO>()
                .eq(LlmAgentProfileProbeDO::getTaskId, taskId)
                .orderByDesc(LlmAgentProfileProbeDO::getCreatedAt)
                .last("LIMIT 1")));
    }

    public Page<LlmAgentProfileProbeDO> pageByTaskId(Long taskId, int page, int size, String profileVersion) {
        Page<LlmAgentProfileProbeDO> pager = new Page<>(page, size);
        LambdaQueryWrapper<LlmAgentProfileProbeDO> query = new LambdaQueryWrapper<LlmAgentProfileProbeDO>()
                .eq(LlmAgentProfileProbeDO::getTaskId, taskId)
                .orderByDesc(LlmAgentProfileProbeDO::getCreatedAt);
        if (profileVersion != null && !profileVersion.isBlank()) {
            query.eq(LlmAgentProfileProbeDO::getProfileVersion, profileVersion);
        }
        mapper.selectPage(pager, query);
        return pager;
    }

    public List<LlmAgentProfileProbeDO> findLatestPerTask() {
        return mapper.selectLatestPerTask();
    }

    public long countLatestByStatus(String status) {
        return mapper.countLatestByStatus(status);
    }

    public Optional<LlmAgentProfileProbeDO> findLatestByTaskIdAndVersion(Long taskId, String profileVersion) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmAgentProfileProbeDO>()
                .eq(LlmAgentProfileProbeDO::getTaskId, taskId)
                .eq(LlmAgentProfileProbeDO::getProfileVersion, profileVersion)
                .orderByDesc(LlmAgentProfileProbeDO::getCreatedAt)
                .last("LIMIT 1")));
    }

    public List<LlmAgentProfileProbeDO> listSince(Long taskId, LocalDateTime since, String profileVersion) {
        LambdaQueryWrapper<LlmAgentProfileProbeDO> query = new LambdaQueryWrapper<LlmAgentProfileProbeDO>()
                .eq(LlmAgentProfileProbeDO::getTaskId, taskId)
                .ge(LlmAgentProfileProbeDO::getCreatedAt, since)
                .orderByAsc(LlmAgentProfileProbeDO::getCreatedAt);
        if (profileVersion != null && !profileVersion.isBlank()) {
            query.eq(LlmAgentProfileProbeDO::getProfileVersion, profileVersion);
        }
        return mapper.selectList(query);
    }

    public List<LlmAgentProfileProbeDO> listAllForExport(Long taskId, String profileVersion, int limit) {
        LambdaQueryWrapper<LlmAgentProfileProbeDO> query = new LambdaQueryWrapper<LlmAgentProfileProbeDO>()
                .eq(LlmAgentProfileProbeDO::getTaskId, taskId)
                .orderByDesc(LlmAgentProfileProbeDO::getCreatedAt)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 1000));
        if (profileVersion != null && !profileVersion.isBlank()) {
            query.eq(LlmAgentProfileProbeDO::getProfileVersion, profileVersion);
        }
        return mapper.selectList(query);
    }
}
