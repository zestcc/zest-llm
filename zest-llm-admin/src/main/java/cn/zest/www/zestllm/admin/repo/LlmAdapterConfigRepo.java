package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmAdapterConfigMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmAdapterConfigDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmAdapterConfigRepo {

    private final LlmAdapterConfigMapper mapper;

    public List<LlmAdapterConfigDO> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<LlmAdapterConfigDO>()
                .orderByAsc(LlmAdapterConfigDO::getConfigKey));
    }

    public Optional<LlmAdapterConfigDO> findByConfigKey(String configKey) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmAdapterConfigDO>()
                .eq(LlmAdapterConfigDO::getConfigKey, configKey)));
    }

    public void insert(LlmAdapterConfigDO entity) {
        mapper.insert(entity);
    }

    public void update(LlmAdapterConfigDO entity) {
        mapper.updateById(entity);
    }
}
