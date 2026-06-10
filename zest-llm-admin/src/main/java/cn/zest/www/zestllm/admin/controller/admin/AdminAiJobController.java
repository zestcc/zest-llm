package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.vo.AiJobOverviewVO;
import cn.zest.www.zestllm.admin.service.AiJobOverviewService;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ai-jobs")
@RequiredArgsConstructor
public class AdminAiJobController {

    private final AiJobOverviewService aiJobOverviewService;

    @GetMapping("/overview")
    public Result<List<AiJobOverviewVO>> overview() {
        return Result.success(aiJobOverviewService.listOverview());
    }
}
