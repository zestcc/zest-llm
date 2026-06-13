package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmGatewayModelMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmGatewayModelDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmGatewayModelRepo {

    private final LlmGatewayModelMapper mapper;

    public Optional<LlmGatewayModelDO> findByModelName(String modelName) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmGatewayModelDO>()
                .eq(LlmGatewayModelDO::getModelName, modelName)));
    }

    public Optional<LlmGatewayModelDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    public List<LlmGatewayModelDO> findAllActive() {
        return mapper.selectList(new LambdaQueryWrapper<LlmGatewayModelDO>()
                .eq(LlmGatewayModelDO::getStatus, "ACTIVE")
                .orderByAsc(LlmGatewayModelDO::getSortOrder)
                .orderByAsc(LlmGatewayModelDO::getModelName));
    }

    public List<LlmGatewayModelDO> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<LlmGatewayModelDO>()
                .orderByAsc(LlmGatewayModelDO::getSortOrder)
                .orderByAsc(LlmGatewayModelDO::getModelName));
    }

    public void insert(LlmGatewayModelDO entity) {
        mapper.insert(entity);
    }

    public void update(LlmGatewayModelDO entity) {
        mapper.updateById(entity);
    }
}
