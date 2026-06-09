package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAuditLogDO;
import cn.zest.www.zestllm.admin.model.vo.AuditLogVO;
import cn.zest.www.zestllm.admin.repo.LlmAuditLogRepo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final LlmAuditLogRepo auditLogRepo;

    public Page<AuditLogVO> page(int pageNum, int pageSize, String action, String resourceType) {
        Page<LlmAuditLogDO> page = auditLogRepo.page(pageNum, pageSize, action, resourceType);
        Page<AuditLogVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    private AuditLogVO toVO(LlmAuditLogDO entity) {
        return AuditLogVO.builder()
                .id(entity.getId())
                .actor(entity.getActor())
                .action(entity.getAction())
                .resourceType(entity.getResourceType())
                .resourceId(entity.getResourceId())
                .detailJson(entity.getDetailJson())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
