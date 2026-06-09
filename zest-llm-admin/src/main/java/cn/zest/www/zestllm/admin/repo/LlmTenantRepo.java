package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmTenantMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmTenantDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmTenantRepo {

    private final LlmTenantMapper mapper;

    public Optional<LlmTenantDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    public Optional<LlmTenantDO> findByTenantCode(String tenantCode) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmTenantDO>()
                .eq(LlmTenantDO::getTenantCode, tenantCode)));
    }

    public List<LlmTenantDO> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<LlmTenantDO>()
                .orderByAsc(LlmTenantDO::getTenantCode));
    }

    public void insert(LlmTenantDO entity) {
        mapper.insert(entity);
    }
}
