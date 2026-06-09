package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.CreateTenantRequest;
import cn.zest.www.zestllm.admin.model.vo.TenantVO;
import cn.zest.www.zestllm.admin.service.TenantManageService;
import com.zestflow.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
public class AdminTenantController {

    private final TenantManageService tenantManageService;

    @GetMapping
    public Result<List<TenantVO>> list() {
        return Result.success(tenantManageService.listAll());
    }

    @PostMapping
    public Result<TenantVO> create(@Valid @RequestBody CreateTenantRequest request) {
        return Result.success(tenantManageService.create(request));
    }
}
