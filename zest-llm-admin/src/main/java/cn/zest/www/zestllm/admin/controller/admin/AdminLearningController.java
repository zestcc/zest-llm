package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.LearningRunCycleRequest;
import cn.zest.www.zestllm.admin.model.request.LearningSuggestCasesRequest;
import cn.zest.www.zestllm.admin.model.vo.LearningCycleRunVO;
import cn.zest.www.zestllm.admin.service.LearningManageService;
import cn.zest.www.zestllm.spi.learning.EvalCaseSuggestion;
import cn.zest.www.zestllm.spi.learning.LearningCycleResult;
import cn.zest.www.zestllm.spi.learning.TraceSampleQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/learning")
@RequiredArgsConstructor
public class AdminLearningController {

    private final LearningManageService learningManageService;

    @PostMapping("/suggest-cases")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<List<EvalCaseSuggestion>> suggestCases(@RequestBody LearningSuggestCasesRequest request) {
        TraceSampleQuery query = TraceSampleQuery.builder()
                .taskCode(request.getTaskCode())
                .since(request.getSince())
                .limit(request.getLimit())
                .distillationSources(request.getDistillationSources())
                .build();
        return Result.success(learningManageService.suggestCases(query));
    }

    @PostMapping("/run-cycle")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<LearningCycleResult> runCycle(@RequestBody LearningRunCycleRequest request) {
        return Result.success(learningManageService.runCycle(
                request.getTaskCode(), request.getProfileVersion(), request.isDryRun()));
    }

    @GetMapping("/cycles")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<Page<LearningCycleRunVO>> listCycles(
            @RequestParam(required = false) String taskCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(learningManageService.pageCycles(taskCode, page, size));
    }
}
