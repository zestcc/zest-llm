package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmMethodRegistryMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmMethodRegistryDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmMethodRegistryRepo {

    private final LlmMethodRegistryMapper mapper;

    public Optional<LlmMethodRegistryDO> findByAppIdAndCode(Long appId, String code) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmMethodRegistryDO>()
                .eq(LlmMethodRegistryDO::getAppId, appId)
                .eq(LlmMethodRegistryDO::getCode, code)));
    }

    public void insert(LlmMethodRegistryDO entity) {
        mapper.insert(entity);
    }

    public void update(LlmMethodRegistryDO entity) {
        mapper.updateById(entity);
    }

    public Page<LlmMethodRegistryDO> pageAll(int pageNum, int pageSize) {
        return mapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<LlmMethodRegistryDO>()
                        .orderByDesc(LlmMethodRegistryDO::getRegisteredAt));
    }

    public Page<LlmMethodRegistryDO> pageByAppId(int pageNum, int pageSize, Long appId) {
        return mapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<LlmMethodRegistryDO>()
                        .eq(LlmMethodRegistryDO::getAppId, appId)
                        .orderByDesc(LlmMethodRegistryDO::getRegisteredAt));
    }
}
