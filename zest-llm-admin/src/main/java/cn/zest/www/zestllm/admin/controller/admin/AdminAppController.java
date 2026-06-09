package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.CreateAppRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateAppRequest;
import cn.zest.www.zestllm.admin.model.vo.AppVO;
import cn.zest.www.zestllm.admin.model.vo.RotateTokenVO;
import cn.zest.www.zestllm.admin.service.AppManageService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zestflow.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/apps")
@RequiredArgsConstructor
public class AdminAppController {

    private final AppManageService appManageService;

    @GetMapping
    public Result<Page<AppVO>> listApps(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(appManageService.page(page, size));
    }

    @PostMapping
    public Result<AppVO> createApp(@Valid @RequestBody CreateAppRequest request) {
        return Result.success(appManageService.create(request));
    }

    @PutMapping("/{appKey}")
    public Result<AppVO> updateApp(@PathVariable String appKey,
                                   @Valid @RequestBody UpdateAppRequest request) {
        return Result.success(appManageService.update(appKey, request));
    }

    @PostMapping("/{appKey}/rotate-token")
    public Result<RotateTokenVO> rotateToken(@PathVariable String appKey) {
        return Result.success(appManageService.rotateToken(appKey));
    }

    @DeleteMapping("/{appKey}")
    public Result<Void> deleteApp(@PathVariable String appKey) {
        appManageService.disable(appKey);
        return Result.success(null);
    }
}
