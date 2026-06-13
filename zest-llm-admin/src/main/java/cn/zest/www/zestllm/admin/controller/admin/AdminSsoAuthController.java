package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.AdminOidcCallbackRequest;
import cn.zest.www.zestllm.admin.model.request.AdminOidcExchangeRequest;
import cn.zest.www.zestllm.admin.model.vo.AdminLoginVO;
import cn.zest.www.zestllm.admin.model.vo.AdminOidcAuthorizeVO;
import cn.zest.www.zestllm.admin.model.vo.AdminOidcConfigVO;
import cn.zest.www.zestllm.admin.service.sso.AdminSsoAuthService;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth/sso")
@RequiredArgsConstructor
public class AdminSsoAuthController {

    private final AdminSsoAuthService adminSsoAuthService;

    @GetMapping("/config")
    public Result<AdminOidcConfigVO> config() {
        return Result.success(adminSsoAuthService.getPublicConfig());
    }

    @GetMapping("/authorize")
    public Result<AdminOidcAuthorizeVO> authorize() {
        return Result.success(adminSsoAuthService.buildAuthorizeUrl());
    }

    @PostMapping("/callback")
    public Result<AdminLoginVO> callback(@RequestBody AdminOidcCallbackRequest request) {
        return Result.success(adminSsoAuthService.handleCallback(request));
    }

    @PostMapping("/exchange")
    public Result<AdminLoginVO> exchange(@RequestBody AdminOidcExchangeRequest request) {
        return Result.success(adminSsoAuthService.exchangeIdToken(request));
    }

    @GetMapping("/logout-url")
    public Result<String> logoutUrl() {
        return Result.success(adminSsoAuthService.buildLogoutUrl());
    }
}
