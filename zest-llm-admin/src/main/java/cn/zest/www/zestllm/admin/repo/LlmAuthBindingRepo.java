package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmAuthBindingMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmAuthBindingDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmAuthBindingRepo {

    private final LlmAuthBindingMapper mapper;

    public Optional<LlmAuthBindingDO> findByScope(String scopeType, Long scopeId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmAuthBindingDO>()
                .eq(LlmAuthBindingDO::getScopeType, scopeType)
                .eq(LlmAuthBindingDO::getScopeId, scopeId)
                .eq(LlmAuthBindingDO::getStatus, "ACTIVE")));
    }

    public void insert(LlmAuthBindingDO entity) {
        mapper.insert(entity);
    }

    public void update(LlmAuthBindingDO entity) {
        mapper.updateById(entity);
    }
}
