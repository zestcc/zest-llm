package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmSecretRefMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmSecretRefDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmSecretRefRepo {

    private final LlmSecretRefMapper mapper;

    public Optional<LlmSecretRefDO> findByCode(String secretCode) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmSecretRefDO>()
                .eq(LlmSecretRefDO::getSecretCode, secretCode)
                .eq(LlmSecretRefDO::getStatus, "ACTIVE")));
    }

    public Optional<LlmSecretRefDO> findByCodeAnyStatus(String secretCode) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmSecretRefDO>()
                .eq(LlmSecretRefDO::getSecretCode, secretCode)));
    }

    public List<LlmSecretRefDO> findAllActive() {
        return mapper.selectList(new LambdaQueryWrapper<LlmSecretRefDO>()
                .eq(LlmSecretRefDO::getStatus, "ACTIVE")
                .orderByAsc(LlmSecretRefDO::getSecretCode));
    }

    public void insert(LlmSecretRefDO entity) {
        mapper.insert(entity);
    }

    public void update(LlmSecretRefDO entity) {
        mapper.updateById(entity);
    }
}
