package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.vo.AdminBuildInfoVO;
import cn.zest.www.zestllm.admin.model.vo.AdminFeaturesVO;
import cn.zest.www.zestllm.admin.service.AdminMetaService;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/meta")
@RequiredArgsConstructor
public class AdminMetaController {

    private final AdminMetaService adminMetaService;

    @GetMapping("/features")
    public Result<AdminFeaturesVO> features() {
        return Result.success(adminMetaService.features());
    }

    @GetMapping("/build")
    public Result<AdminBuildInfoVO> build() {
        return Result.success(adminMetaService.buildInfo());
    }
}
