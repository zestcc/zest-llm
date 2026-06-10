package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.UpsertAuthBindingRequest;
import cn.zest.www.zestllm.admin.model.vo.AuthBindingVO;
import cn.zest.www.zestllm.admin.service.AuthBindingManageService;
import com.zestflow.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth-bindings")
@RequiredArgsConstructor
public class AdminAuthBindingController {

    private final AuthBindingManageService authBindingManageService;

    @GetMapping("/apps/{appKey}")
    public Result<AuthBindingVO> getByApp(@PathVariable String appKey) {
        return Result.success(authBindingManageService.getByAppKey(appKey));
    }

    @PutMapping
    public Result<AuthBindingVO> upsert(@Valid @RequestBody UpsertAuthBindingRequest request) {
        return Result.success(authBindingManageService.upsert(request));
    }
}
