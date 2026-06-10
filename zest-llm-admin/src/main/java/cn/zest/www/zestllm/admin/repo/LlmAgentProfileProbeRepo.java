package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmAgentProfileProbeMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileProbeDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
}
