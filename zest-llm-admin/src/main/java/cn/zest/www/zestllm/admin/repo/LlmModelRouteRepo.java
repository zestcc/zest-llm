package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmModelRouteMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmModelRouteDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmModelRouteRepo {

    private final LlmModelRouteMapper mapper;

    public Optional<LlmModelRouteDO> findActiveByTaskId(Long taskId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmModelRouteDO>()
                .eq(LlmModelRouteDO::getTaskId, taskId)
                .eq(LlmModelRouteDO::getStatus, "ACTIVE")
                .last("LIMIT 1")));
    }

    public Optional<LlmModelRouteDO> findByTaskId(Long taskId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmModelRouteDO>()
                .eq(LlmModelRouteDO::getTaskId, taskId)
                .last("LIMIT 1")));
    }

    public List<LlmModelRouteDO> findByTaskIds(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<LlmModelRouteDO>()
                .in(LlmModelRouteDO::getTaskId, taskIds));
    }

    public void insert(LlmModelRouteDO entity) {
        mapper.insert(entity);
    }

    public void update(LlmModelRouteDO entity) {
        mapper.updateById(entity);
    }
}
