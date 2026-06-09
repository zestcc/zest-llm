package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmAiTaskDefMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmAiTaskDefRepo {

    private final LlmAiTaskDefMapper mapper;

    public Optional<LlmAiTaskDefDO> findByAppIdAndCode(Long appId, String code) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmAiTaskDefDO>()
                .eq(LlmAiTaskDefDO::getAppId, appId)
                .eq(LlmAiTaskDefDO::getCode, code)));
    }

    public Optional<LlmAiTaskDefDO> findByCode(String code) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmAiTaskDefDO>()
                .eq(LlmAiTaskDefDO::getCode, code)
                .last("LIMIT 1")));
    }

    public List<LlmAiTaskDefDO> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<LlmAiTaskDefDO>().orderByAsc(LlmAiTaskDefDO::getCode));
    }

    public List<LlmAiTaskDefDO> findByAppId(Long appId) {
        return mapper.selectList(new LambdaQueryWrapper<LlmAiTaskDefDO>()
                .eq(LlmAiTaskDefDO::getAppId, appId)
                .orderByAsc(LlmAiTaskDefDO::getCode));
    }

    public Page<LlmAiTaskDefDO> page(int pageNum, int pageSize, Long appId) {
        LambdaQueryWrapper<LlmAiTaskDefDO> wrapper = new LambdaQueryWrapper<LlmAiTaskDefDO>()
                .orderByAsc(LlmAiTaskDefDO::getCode);
        if (appId != null) {
            wrapper.eq(LlmAiTaskDefDO::getAppId, appId);
        }
        return mapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public void insert(LlmAiTaskDefDO entity) {
        mapper.insert(entity);
    }

    public void update(LlmAiTaskDefDO entity) {
        mapper.updateById(entity);
    }
}
