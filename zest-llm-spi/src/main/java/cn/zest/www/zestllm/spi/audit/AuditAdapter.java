package cn.zest.www.zestllm.spi.audit;

public interface AuditAdapter {

    String adapterId();

    void logAdminAction(AdminAuditEvent event);
}
