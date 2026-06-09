package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmAppMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmAppRepo {

    private final LlmAppMapper mapper;

    public Optional<LlmAppDO> findByAppKey(String appKey) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmAppDO>()
                .eq(LlmAppDO::getAppKey, appKey)));
    }

    public Optional<LlmAppDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    public List<LlmAppDO> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<LlmAppDO>().orderByAsc(LlmAppDO::getAppKey));
    }

    public Page<LlmAppDO> page(int pageNum, int pageSize) {
        return mapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<LlmAppDO>().orderByAsc(LlmAppDO::getAppKey));
    }

    public long countActive() {
        return mapper.selectCount(new LambdaQueryWrapper<LlmAppDO>()
                .eq(LlmAppDO::getStatus, "ACTIVE"));
    }

    public void insert(LlmAppDO entity) {
        mapper.insert(entity);
    }

    public void update(LlmAppDO entity) {
        mapper.updateById(entity);
    }
}
