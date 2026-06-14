package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmIntegrationWebhookDeliveryMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmIntegrationWebhookDeliveryDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmIntegrationWebhookDeliveryRepo {

    private final LlmIntegrationWebhookDeliveryMapper mapper;

    public void insert(LlmIntegrationWebhookDeliveryDO entity) {
        mapper.insert(entity);
    }

    public void updateById(LlmIntegrationWebhookDeliveryDO entity) {
        mapper.updateById(entity);
    }

    public Optional<LlmIntegrationWebhookDeliveryDO> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    public Page<LlmIntegrationWebhookDeliveryDO> page(String taskCode, int pageNum, int pageSize) {
        Page<LlmIntegrationWebhookDeliveryDO> pager = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<LlmIntegrationWebhookDeliveryDO> query = new LambdaQueryWrapper<LlmIntegrationWebhookDeliveryDO>()
                .orderByDesc(LlmIntegrationWebhookDeliveryDO::getCreatedAt);
        if (taskCode != null && !taskCode.isBlank()) {
            query.eq(LlmIntegrationWebhookDeliveryDO::getTaskCode, taskCode);
        }
        mapper.selectPage(pager, query);
        return pager;
    }
}
