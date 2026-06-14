package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.vo.IntegrationSetupChecklistVO;
import cn.zest.www.zestllm.admin.service.IntegrationSetupGuideService;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/adapters")
@RequiredArgsConstructor
public class AdminIntegrationSetupController {

    private final IntegrationSetupGuideService integrationSetupGuideService;

    @GetMapping("/setup-guide")
    public Result<IntegrationSetupChecklistVO> setupGuide() {
        return Result.success(integrationSetupGuideService.buildChecklist());
    }
}
