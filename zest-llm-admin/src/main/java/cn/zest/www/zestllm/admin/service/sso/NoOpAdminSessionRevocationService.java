package cn.zest.www.zestllm.admin.service.sso;

import cn.zest.www.zestllm.spi.adminsso.AdminSsoSessionRevocation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * 无 Redis 时的会话吊销占位实现（本地开发默认可用）。
 */
@Service
@ConditionalOnMissingBean(AdminSessionRevocationService.class)
public class NoOpAdminSessionRevocationService implements AdminSsoSessionRevocation {

    @Override
    public void revokeByUsername(String username) {
    }
}
