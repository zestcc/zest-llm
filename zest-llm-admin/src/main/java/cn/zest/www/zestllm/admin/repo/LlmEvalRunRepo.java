package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmEvalRunMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmEvalRunDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmEvalRunRepo {
    private final LlmEvalRunMapper mapper;

    public void insert(LlmEvalRunDO entity) {
        mapper.insert(entity);
    }

    public void updateById(LlmEvalRunDO entity) {
        mapper.updateById(entity);
    }

    public Optional<LlmEvalRunDO> findByRunCode(String runCode) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmEvalRunDO>()
                .eq(LlmEvalRunDO::getRunCode, runCode)));
    }

    public List<LlmEvalRunDO> findByDatasetId(Long datasetId) {
        return mapper.selectList(new LambdaQueryWrapper<LlmEvalRunDO>()
                .eq(LlmEvalRunDO::getDatasetId, datasetId)
                .orderByDesc(LlmEvalRunDO::getStartedAt));
    }
}
