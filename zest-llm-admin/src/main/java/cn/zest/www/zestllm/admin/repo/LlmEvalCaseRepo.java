package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmEvalCaseMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmEvalCaseDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmEvalCaseRepo {
    private final LlmEvalCaseMapper mapper;

    public List<LlmEvalCaseDO> findActiveByDatasetId(Long datasetId) {
        return mapper.selectList(new LambdaQueryWrapper<LlmEvalCaseDO>()
                .eq(LlmEvalCaseDO::getDatasetId, datasetId)
                .eq(LlmEvalCaseDO::getStatus, "ACTIVE"));
    }

    public void insert(LlmEvalCaseDO entity) {
        mapper.insert(entity);
    }

    public Optional<LlmEvalCaseDO> findByDatasetAndCode(Long datasetId, String caseCode) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmEvalCaseDO>()
                .eq(LlmEvalCaseDO::getDatasetId, datasetId)
                .eq(LlmEvalCaseDO::getCaseCode, caseCode)));
    }
}
