package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.request.AgentProfileProbeRequest;
import cn.zest.www.zestllm.admin.model.request.AiJobWizardRequest;
import cn.zest.www.zestllm.admin.model.request.ApplyScenarioTemplateRequest;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeResultVO;
import cn.zest.www.zestllm.admin.model.vo.AiJobWizardResultVO;
import cn.zest.www.zestllm.admin.model.vo.ApplyScenarioTemplateResultVO;
import cn.zest.www.zestllm.admin.model.vo.ScenarioTemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiJobWizardService {

    private final ScenarioTemplateService scenarioTemplateService;
    private final AgentProfileProbeService agentProfileProbeService;

    public AiJobWizardResultVO run(AiJobWizardRequest request) {
        ScenarioTemplateVO template = scenarioTemplateService.getTemplate(request.getTemplateId());
        ApplyScenarioTemplateRequest apply = new ApplyScenarioTemplateRequest();
        apply.setTemplateId(request.getTemplateId());
        apply.setAppKey(request.getAppKey());
        apply.setTaskCode(StringUtils.hasText(request.getTaskCode())
                ? request.getTaskCode()
                : template.getTaskCodeSuggestion());
        apply.setPublish(request.isPublish());

        ApplyScenarioTemplateResultVO applied = scenarioTemplateService.apply(apply);
        String probeStatus = "SKIPPED";
        if (request.isRunProbe()) {
            AgentProfileProbeResultVO probe = agentProfileProbeService.probeVersion(
                    applied.getTaskCode(), applied.getProfileVersion(), new AgentProfileProbeRequest());
            probeStatus = probe.getOverallStatus();
        }

        return AiJobWizardResultVO.builder()
                .taskCode(applied.getTaskCode())
                .profileVersion(applied.getProfileVersion())
                .published(applied.isPublished())
                .probeStatus(probeStatus)
                .scenarioName(template.getName())
                .recommendedTier(template.getRecommendedTier())
                .nextUrl("/agent-config?task=" + applied.getTaskCode())
                .message(applied.getMessage())
                .build();
    }
}
