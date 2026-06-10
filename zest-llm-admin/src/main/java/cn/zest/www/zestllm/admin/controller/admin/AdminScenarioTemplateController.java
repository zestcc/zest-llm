package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.ApplyScenarioTemplateRequest;
import cn.zest.www.zestllm.admin.model.vo.ApplyScenarioTemplateResultVO;
import cn.zest.www.zestllm.admin.model.vo.ScenarioTemplateVO;
import cn.zest.www.zestllm.admin.service.ScenarioTemplateService;
import com.zestflow.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/scenario-templates")
@RequiredArgsConstructor
public class AdminScenarioTemplateController {

    private final ScenarioTemplateService scenarioTemplateService;

    @GetMapping
    public Result<List<ScenarioTemplateVO>> list() {
        return Result.success(scenarioTemplateService.listTemplates());
    }

    @GetMapping("/{templateId}")
    public Result<ScenarioTemplateVO> get(@PathVariable String templateId) {
        return Result.success(scenarioTemplateService.getTemplate(templateId));
    }

    @GetMapping("/{templateId}/profile")
    public Result<Map<String, String>> exportProfile(@PathVariable String templateId) {
        return Result.success(Map.of("profileJson", scenarioTemplateService.exportProfileJson(templateId)));
    }

    @PostMapping("/apply")
    public Result<ApplyScenarioTemplateResultVO> apply(@Valid @RequestBody ApplyScenarioTemplateRequest request) {
        return Result.success(scenarioTemplateService.apply(request));
    }
}
