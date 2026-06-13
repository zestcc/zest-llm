package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.AdminOidcCallbackRequest;
import cn.zest.www.zestllm.admin.model.request.AdminOidcExchangeRequest;
import cn.zest.www.zestllm.admin.model.vo.AdminLoginVO;
import cn.zest.www.zestllm.admin.model.vo.AdminOidcAuthorizeVO;
import cn.zest.www.zestllm.admin.model.vo.AdminOidcConfigVO;
import cn.zest.www.zestllm.admin.service.AdminOidcAuthService;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth/oidc")
@RequiredArgsConstructor
public class AdminOidcAuthController {

    private final AdminOidcAuthService adminOidcAuthService;

    @GetMapping("/config")
    public Result<AdminOidcConfigVO> config() {
        return Result.success(adminOidcAuthService.getPublicConfig());
    }

    @GetMapping("/authorize")
    public Result<AdminOidcAuthorizeVO> authorize() {
        return Result.success(adminOidcAuthService.buildAuthorizeUrl());
    }

    @PostMapping("/callback")
    public Result<AdminLoginVO> callback(@RequestBody AdminOidcCallbackRequest request) {
        return Result.success(adminOidcAuthService.handleCallback(request));
    }

    @PostMapping("/exchange")
    public Result<AdminLoginVO> exchange(@RequestBody AdminOidcExchangeRequest request) {
        return Result.success(adminOidcAuthService.exchangeIdToken(request));
    }

    @GetMapping("/logout-url")
    public Result<String> logoutUrl() {
        return Result.success(adminOidcAuthService.buildLogoutUrl());
    }
}
