package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmAppQuotaMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmAppQuotaDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmAppQuotaRepo {

    private final LlmAppQuotaMapper mapper;

    public Optional<LlmAppQuotaDO> findByAppId(Long appId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmAppQuotaDO>()
                .eq(LlmAppQuotaDO::getAppId, appId)));
    }

    public void insert(LlmAppQuotaDO entity) {
        mapper.insert(entity);
    }

    public void update(LlmAppQuotaDO entity) {
        mapper.updateById(entity);
    }
}
