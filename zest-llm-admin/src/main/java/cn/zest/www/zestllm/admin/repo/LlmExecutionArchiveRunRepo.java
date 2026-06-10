package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmExecutionArchiveRunMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmExecutionArchiveRunDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LlmExecutionArchiveRunRepo {

    private final LlmExecutionArchiveRunMapper mapper;

    public void insert(LlmExecutionArchiveRunDO entity) {
        mapper.insert(entity);
    }

    public Page<LlmExecutionArchiveRunDO> page(int pageNum, int pageSize) {
        Page<LlmExecutionArchiveRunDO> pager = new Page<>(pageNum, pageSize);
        mapper.selectPage(pager, new LambdaQueryWrapper<LlmExecutionArchiveRunDO>()
                .orderByDesc(LlmExecutionArchiveRunDO::getCreatedAt));
        return pager;
    }
}
