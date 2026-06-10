package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmEvalDatasetMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmEvalDatasetDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmEvalDatasetRepo {
    private final LlmEvalDatasetMapper mapper;

    public List<LlmEvalDatasetDO> findAllActive() {
        return mapper.selectList(new LambdaQueryWrapper<LlmEvalDatasetDO>()
                .eq(LlmEvalDatasetDO::getStatus, "ACTIVE")
                .orderByAsc(LlmEvalDatasetDO::getDatasetCode));
    }

    public Optional<LlmEvalDatasetDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    public Optional<LlmEvalDatasetDO> findByCode(String datasetCode) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmEvalDatasetDO>()
                .eq(LlmEvalDatasetDO::getDatasetCode, datasetCode)));
    }

    public void insert(LlmEvalDatasetDO entity) {
        mapper.insert(entity);
    }
}
