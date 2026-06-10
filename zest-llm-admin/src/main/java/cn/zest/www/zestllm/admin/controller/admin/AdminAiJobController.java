package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.AiJobWizardRequest;
import cn.zest.www.zestllm.admin.model.vo.AiJobOverviewVO;
import cn.zest.www.zestllm.admin.model.vo.AiJobWizardResultVO;
import cn.zest.www.zestllm.admin.service.AiJobOverviewService;
import cn.zest.www.zestllm.admin.service.AiJobWizardService;
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
@RequestMapping("/api/admin/ai-jobs")
@RequiredArgsConstructor
public class AdminAiJobController {

    private final AiJobOverviewService aiJobOverviewService;
    private final AiJobWizardService aiJobWizardService;

    @GetMapping("/overview")
    public Result<List<AiJobOverviewVO>> overview() {
        return Result.success(aiJobOverviewService.listOverview());
    }

    @PostMapping("/wizard")
    public Result<AiJobWizardResultVO> wizard(@Valid @RequestBody AiJobWizardRequest request) {
        return Result.success(aiJobWizardService.run(request));
    }
}
