package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmFlowChainMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmFlowChainDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmFlowChainRepo {
    private final LlmFlowChainMapper mapper;

    public List<LlmFlowChainDO> findAllActive() {
        return mapper.selectList(new LambdaQueryWrapper<LlmFlowChainDO>()
                .eq(LlmFlowChainDO::getStatus, "ACTIVE"));
    }

    public Optional<LlmFlowChainDO> findByChainCode(String chainCode) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmFlowChainDO>()
                .eq(LlmFlowChainDO::getChainCode, chainCode)));
    }
}
