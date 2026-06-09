package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmPromptVersionMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmPromptVersionDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmPromptVersionRepo {

    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_DRAFT = "DRAFT";

    private final LlmPromptVersionMapper mapper;

    public Optional<LlmPromptVersionDO> findPublishedByTaskId(Long taskId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmPromptVersionDO>()
                .eq(LlmPromptVersionDO::getTaskId, taskId)
                .eq(LlmPromptVersionDO::getStatus, STATUS_PUBLISHED)
                .orderByDesc(LlmPromptVersionDO::getPublishedAt)
                .last("LIMIT 1")));
    }

    public Optional<LlmPromptVersionDO> findByTaskIdAndVersion(Long taskId, String version) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmPromptVersionDO>()
                .eq(LlmPromptVersionDO::getTaskId, taskId)
                .eq(LlmPromptVersionDO::getVersion, version)));
    }

    public List<LlmPromptVersionDO> findByTaskId(Long taskId) {
        return mapper.selectList(new LambdaQueryWrapper<LlmPromptVersionDO>()
                .eq(LlmPromptVersionDO::getTaskId, taskId)
                .orderByDesc(LlmPromptVersionDO::getCreatedAt));
    }

    public void unpublishOthers(Long taskId, String keepVersion) {
        mapper.update(null, new LambdaUpdateWrapper<LlmPromptVersionDO>()
                .eq(LlmPromptVersionDO::getTaskId, taskId)
                .eq(LlmPromptVersionDO::getStatus, STATUS_PUBLISHED)
                .ne(LlmPromptVersionDO::getVersion, keepVersion)
                .set(LlmPromptVersionDO::getStatus, STATUS_DRAFT)
                .set(LlmPromptVersionDO::getUpdatedAt, LocalDateTime.now()));
    }

    public void publish(Long taskId, String version, String operator) {
        mapper.update(null, new LambdaUpdateWrapper<LlmPromptVersionDO>()
                .eq(LlmPromptVersionDO::getTaskId, taskId)
                .eq(LlmPromptVersionDO::getVersion, version)
                .set(LlmPromptVersionDO::getStatus, STATUS_PUBLISHED)
                .set(LlmPromptVersionDO::getPublishedAt, LocalDateTime.now())
                .set(LlmPromptVersionDO::getCreatedBy, operator)
                .set(LlmPromptVersionDO::getUpdatedAt, LocalDateTime.now()));
    }

    public void insert(LlmPromptVersionDO entity) {
        mapper.insert(entity);
    }
}
