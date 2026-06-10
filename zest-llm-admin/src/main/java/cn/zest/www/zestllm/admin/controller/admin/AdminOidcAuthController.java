package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.AdminOidcExchangeRequest;
import cn.zest.www.zestllm.admin.model.vo.AdminLoginVO;
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

    @PostMapping("/exchange")
    public Result<AdminLoginVO> exchange(@RequestBody AdminOidcExchangeRequest request) {
        return Result.success(adminOidcAuthService.exchangeIdToken(request));
    }
}
