package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmAgentProfileMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmAgentProfileRepo {

    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_DRAFT = "DRAFT";

    private final LlmAgentProfileMapper mapper;

    public Optional<LlmAgentProfileDO> findPublishedByTaskId(Long taskId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmAgentProfileDO>()
                .eq(LlmAgentProfileDO::getTaskId, taskId)
                .eq(LlmAgentProfileDO::getStatus, STATUS_PUBLISHED)
                .orderByDesc(LlmAgentProfileDO::getPublishedAt)
                .last("LIMIT 1")));
    }

    public Optional<LlmAgentProfileDO> findByTaskIdAndVersion(Long taskId, String version) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmAgentProfileDO>()
                .eq(LlmAgentProfileDO::getTaskId, taskId)
                .eq(LlmAgentProfileDO::getVersion, version)));
    }

    public List<LlmAgentProfileDO> findByTaskId(Long taskId) {
        return mapper.selectList(new LambdaQueryWrapper<LlmAgentProfileDO>()
                .eq(LlmAgentProfileDO::getTaskId, taskId)
                .orderByDesc(LlmAgentProfileDO::getCreatedAt));
    }

    public void unpublishOthers(Long taskId, String keepVersion) {
        mapper.update(null, new LambdaUpdateWrapper<LlmAgentProfileDO>()
                .eq(LlmAgentProfileDO::getTaskId, taskId)
                .eq(LlmAgentProfileDO::getStatus, STATUS_PUBLISHED)
                .ne(LlmAgentProfileDO::getVersion, keepVersion)
                .set(LlmAgentProfileDO::getStatus, STATUS_DRAFT)
                .set(LlmAgentProfileDO::getUpdatedAt, LocalDateTime.now()));
    }

    public void publish(Long taskId, String version, String operator) {
        mapper.update(null, new LambdaUpdateWrapper<LlmAgentProfileDO>()
                .eq(LlmAgentProfileDO::getTaskId, taskId)
                .eq(LlmAgentProfileDO::getVersion, version)
                .set(LlmAgentProfileDO::getStatus, STATUS_PUBLISHED)
                .set(LlmAgentProfileDO::getPublishedAt, LocalDateTime.now())
                .set(LlmAgentProfileDO::getCreatedBy, operator)
                .set(LlmAgentProfileDO::getUpdatedAt, LocalDateTime.now()));
    }

    public void insert(LlmAgentProfileDO entity) {
        mapper.insert(entity);
    }

    public void update(LlmAgentProfileDO entity) {
        mapper.updateById(entity);
    }

    public List<LlmAgentProfileDO> findAllPublished() {
        return mapper.selectList(new LambdaQueryWrapper<LlmAgentProfileDO>()
                .eq(LlmAgentProfileDO::getStatus, STATUS_PUBLISHED)
                .orderByAsc(LlmAgentProfileDO::getTaskId));
    }

    public long countPublishedTasks() {
        return mapper.selectCount(new LambdaQueryWrapper<LlmAgentProfileDO>()
                .eq(LlmAgentProfileDO::getStatus, STATUS_PUBLISHED));
    }

    public void deleteByTaskId(Long taskId) {
        mapper.delete(new LambdaQueryWrapper<LlmAgentProfileDO>()
                .eq(LlmAgentProfileDO::getTaskId, taskId));
    }
}
