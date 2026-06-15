package cn.zest.www.zestllm.spi.adminsso;

/**
 * SSO Back-Channel Logout 会话吊销，由 Admin 核心实现，供 identity-zestsso 插件委托。
 */
public interface AdminSsoSessionRevocation {

    void revokeByUsername(String username);

    default void clearRevocation(String username) {
    }

    default boolean isRevoked(String username) {
        return false;
    }
}
