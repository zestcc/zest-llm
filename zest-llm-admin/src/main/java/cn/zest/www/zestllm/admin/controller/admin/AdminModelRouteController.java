package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.UpdateModelRouteRequest;
import cn.zest.www.zestllm.admin.model.vo.ModelRouteVO;
import cn.zest.www.zestllm.admin.service.ModelRouteManageService;
import com.zestflow.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/model-routes")
@RequiredArgsConstructor
public class AdminModelRouteController {

    private final ModelRouteManageService modelRouteManageService;

    @GetMapping
    public Result<List<ModelRouteVO>> listRoutes(@RequestParam(required = false) String taskCode) {
        return Result.success(modelRouteManageService.list(taskCode));
    }

    @PutMapping("/{taskCode}")
    public Result<ModelRouteVO> updateRoute(@PathVariable String taskCode,
                                            @Valid @RequestBody UpdateModelRouteRequest request) {
        return Result.success(modelRouteManageService.update(taskCode, request));
    }
}
