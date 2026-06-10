package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmLearningCycleRunMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmLearningCycleRunDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmLearningCycleRunRepo {

    private final LlmLearningCycleRunMapper mapper;

    public void insert(LlmLearningCycleRunDO entity) {
        mapper.insert(entity);
    }

    public void updateById(LlmLearningCycleRunDO entity) {
        mapper.updateById(entity);
    }

    public Optional<LlmLearningCycleRunDO> findByRunCode(String runCode) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmLearningCycleRunDO>()
                .eq(LlmLearningCycleRunDO::getRunCode, runCode)));
    }

    public Page<LlmLearningCycleRunDO> pageByTask(String taskCode, int pageNum, int pageSize) {
        LambdaQueryWrapper<LlmLearningCycleRunDO> wrapper = new LambdaQueryWrapper<LlmLearningCycleRunDO>()
                .orderByDesc(LlmLearningCycleRunDO::getStartedAt);
        if (taskCode != null && !taskCode.isBlank()) {
            wrapper.eq(LlmLearningCycleRunDO::getTaskCode, taskCode);
        }
        return mapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<LlmLearningCycleRunDO> findRecentByTask(String taskCode, int limit) {
        return mapper.selectList(new LambdaQueryWrapper<LlmLearningCycleRunDO>()
                .eq(LlmLearningCycleRunDO::getTaskCode, taskCode)
                .orderByDesc(LlmLearningCycleRunDO::getStartedAt)
                .last("LIMIT " + limit));
    }
}
