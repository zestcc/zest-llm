package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.dto.CreateEvalCaseCommand;
import cn.zest.www.zestllm.admin.model.dto.CreateEvalDatasetCommand;
import cn.zest.www.zestllm.admin.model.dto.UpdateEvalCaseCommand;
import cn.zest.www.zestllm.admin.model.vo.EvalCaseVO;
import cn.zest.www.zestllm.admin.model.vo.EvalDatasetVO;
import cn.zest.www.zestllm.admin.model.vo.EvalRunVO;
import cn.zest.www.zestllm.admin.service.EvalRunService;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/eval")
@RequiredArgsConstructor
public class AdminEvalController {

    private final EvalRunService evalRunService;

    @GetMapping("/datasets")
    public Result<List<EvalDatasetVO>> listDatasets() {
        return Result.success(evalRunService.listDatasets());
    }

    @PostMapping("/datasets")
    public Result<EvalDatasetVO> createDataset(@RequestBody CreateEvalDatasetCommand command) {
        return Result.success(evalRunService.createDataset(command));
    }

    @GetMapping("/datasets/{datasetCode}/cases")
    public Result<List<EvalCaseVO>> listCases(@PathVariable String datasetCode) {
        return Result.success(evalRunService.listCases(datasetCode));
    }

    @PostMapping("/datasets/{datasetCode}/cases")
    public Result<Void> createCase(@PathVariable String datasetCode,
                                   @RequestBody CreateEvalCaseCommand command) {
        evalRunService.createCase(datasetCode, command);
        return Result.success(null);
    }

    @PutMapping("/datasets/{datasetCode}/cases/{caseCode}")
    public Result<EvalCaseVO> updateCase(@PathVariable String datasetCode,
                                         @PathVariable String caseCode,
                                         @RequestBody UpdateEvalCaseCommand command) {
        return Result.success(evalRunService.updateCase(datasetCode, caseCode, command));
    }

    @DeleteMapping("/datasets/{datasetCode}/cases/{caseCode}")
    public Result<Void> deleteCase(@PathVariable String datasetCode,
                                   @PathVariable String caseCode) {
        evalRunService.deleteCase(datasetCode, caseCode);
        return Result.success(null);
    }

    @GetMapping("/datasets/{datasetCode}/runs")
    public Result<List<EvalRunVO>> listRuns(@PathVariable String datasetCode) {
        return Result.success(evalRunService.listRuns(datasetCode));
    }

    @GetMapping("/runs/{runCode}")
    public Result<EvalRunVO> getRun(@PathVariable String runCode) {
        return Result.success(evalRunService.getRun(runCode));
    }

    @PostMapping("/datasets/{datasetCode}/run")
    public Result<EvalRunVO> runDataset(@PathVariable String datasetCode) {
        return Result.success(evalRunService.runDataset(datasetCode));
    }
}
