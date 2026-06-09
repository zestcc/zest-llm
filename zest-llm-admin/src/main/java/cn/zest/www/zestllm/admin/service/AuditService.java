package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAuditLogDO;
import cn.zest.www.zestllm.admin.repo.LlmAuditLogRepo;
import cn.zest.www.zestllm.spi.audit.AdminAuditEvent;
import cn.zest.www.zestllm.spi.audit.AuditAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final LlmAuditLogRepo auditLogRepo;
    private final AuditAdapter auditAdapter;
    private final ObjectMapper objectMapper;

    public void log(String action, String resourceType, String resourceId, Map<String, Object> detail) {
        AdminAuditEvent event = AdminAuditEvent.builder()
                .operator(currentActor())
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .detail(toJson(detail))
                .timestamp(Instant.now())
                .build();
        if ("jdbc".equals(auditAdapter.adapterId())) {
            auditAdapter.logAdminAction(event);
            return;
        }
        LlmAuditLogDO logEntry = new LlmAuditLogDO();
        logEntry.setActor(event.getOperator());
        logEntry.setAction(action);
        logEntry.setResourceType(resourceType);
        logEntry.setResourceId(resourceId);
        logEntry.setDetailJson(event.getDetail());
        logEntry.setCreatedAt(LocalDateTime.now());
        auditLogRepo.insert(logEntry);
    }

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }
        return "system";
    }

    private String toJson(Map<String, Object> detail) {
        if (detail == null || detail.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize audit detail", ex);
            return detail.toString();
        }
    }
}
