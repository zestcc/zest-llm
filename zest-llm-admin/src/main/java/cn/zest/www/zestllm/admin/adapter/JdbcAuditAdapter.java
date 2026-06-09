package cn.zest.www.zestllm.admin.adapter;

import cn.zest.www.zestllm.admin.model.entity.LlmAuditLogDO;
import cn.zest.www.zestllm.admin.repo.LlmAuditLogRepo;
import cn.zest.www.zestllm.spi.audit.AdminAuditEvent;
import cn.zest.www.zestllm.spi.audit.AuditAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "zest.llm.adapters.audit", havingValue = "jdbc")
public class JdbcAuditAdapter implements AuditAdapter {

    private final LlmAuditLogRepo auditLogRepo;
    private final ObjectMapper objectMapper;

    @Override
    public String adapterId() {
        return "jdbc";
    }

    @Override
    public void logAdminAction(AdminAuditEvent event) {
        LlmAuditLogDO entry = new LlmAuditLogDO();
        entry.setActor(event.getOperator());
        entry.setAction(event.getAction());
        entry.setResourceType(event.getResourceType());
        entry.setResourceId(event.getResourceId());
        entry.setDetailJson(event.getDetail());
        entry.setCreatedAt(LocalDateTime.now());
        auditLogRepo.insert(entry);
    }
}
