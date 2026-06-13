package cn.zest.www.zestllm.admin.service.sso.store;

/**
 * PKCE state → code_verifier 存储。
 */
public interface AdminSsoPkceStore {

    void save(String state, String codeVerifier);

    String consume(String state);
}
