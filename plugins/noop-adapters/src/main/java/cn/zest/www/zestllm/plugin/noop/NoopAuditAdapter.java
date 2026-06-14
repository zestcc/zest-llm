package cn.zest.www.zestllm.plugin.noop;

import cn.zest.www.zestllm.spi.audit.AdminAuditEvent;
import cn.zest.www.zestllm.spi.audit.AuditAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoopAuditAdapter implements AuditAdapter {

    @Override
    public String adapterId() {
        return "noop";
    }

    @Override
    public void logAdminAction(AdminAuditEvent event) {
        log.debug("Audit noop action={} resource={}/{}", event.getAction(), event.getResourceType(), event.getResourceId());
    }
}
