package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmAuditLogMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmAuditLogDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class LlmAuditLogRepo {

    private final LlmAuditLogMapper mapper;

    public void insert(LlmAuditLogDO entity) {
        mapper.insert(entity);
    }

    public Page<LlmAuditLogDO> page(int pageNum, int pageSize, String action, String resourceType) {
        LambdaQueryWrapper<LlmAuditLogDO> wrapper = new LambdaQueryWrapper<LlmAuditLogDO>()
                .orderByDesc(LlmAuditLogDO::getCreatedAt);
        if (StringUtils.hasText(action)) {
            wrapper.eq(LlmAuditLogDO::getAction, action);
        }
        if (StringUtils.hasText(resourceType)) {
            wrapper.eq(LlmAuditLogDO::getResourceType, resourceType);
        }
        return mapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }
}
