package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmProviderPresetMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmProviderPresetDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmProviderPresetRepo {

    private final LlmProviderPresetMapper mapper;

    public Optional<LlmProviderPresetDO> findByCode(String presetCode) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmProviderPresetDO>()
                .eq(LlmProviderPresetDO::getPresetCode, presetCode)
                .eq(LlmProviderPresetDO::getStatus, "ACTIVE")));
    }

    public List<LlmProviderPresetDO> findAllActive() {
        return mapper.selectList(new LambdaQueryWrapper<LlmProviderPresetDO>()
                .eq(LlmProviderPresetDO::getStatus, "ACTIVE")
                .orderByAsc(LlmProviderPresetDO::getSortOrder)
                .orderByAsc(LlmProviderPresetDO::getPresetCode));
    }

    public void insert(LlmProviderPresetDO entity) {
        mapper.insert(entity);
    }

    public void update(LlmProviderPresetDO entity) {
        mapper.updateById(entity);
    }

    public Optional<LlmProviderPresetDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }
}
