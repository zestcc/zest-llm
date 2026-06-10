package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.CreateAdminUserRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateAdminUserRequest;
import cn.zest.www.zestllm.admin.model.vo.AdminUserVO;
import cn.zest.www.zestllm.admin.service.AdminUserManageService;
import com.zestflow.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserManageService adminUserManageService;

    @GetMapping
    public Result<List<AdminUserVO>> list() {
        return Result.success(adminUserManageService.list());
    }

    @PostMapping
    public Result<AdminUserVO> create(@Valid @RequestBody CreateAdminUserRequest request) {
        return Result.success(adminUserManageService.create(request));
    }

    @PutMapping("/{username}")
    public Result<AdminUserVO> update(@PathVariable String username,
                                      @Valid @RequestBody UpdateAdminUserRequest request) {
        return Result.success(adminUserManageService.update(username, request));
    }
}
