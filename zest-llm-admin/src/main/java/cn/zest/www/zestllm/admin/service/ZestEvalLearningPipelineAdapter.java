package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmLearningCycleRunDO;
import cn.zest.www.zestllm.admin.model.request.AgentProfileProbeRequest;
import cn.zest.www.zestllm.admin.model.vo.AgentProfileProbeResultVO;
import cn.zest.www.zestllm.admin.model.vo.EvalRunVO;
import cn.zest.www.zestllm.admin.repo.LlmLearningCycleRunRepo;
import cn.zest.www.zestllm.spi.learning.EvalCaseSuggestion;
import cn.zest.www.zestllm.spi.learning.LearningCycleRequest;
import cn.zest.www.zestllm.spi.learning.LearningCycleResult;
import cn.zest.www.zestllm.spi.learning.LearningPipelineAdapter;
import cn.zest.www.zestllm.spi.learning.TraceSampleQuery;
import cn.zest.www.zestllm.spi.model.HealthStatus;
import cn.zest.www.zestllm.spi.profile.AgentProfileDocument;
import cn.zest.www.zestllm.spi.profile.LearningLoopConfig;
import cn.zest.www.zestllm.spi.profile.ProfileExtensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "zest.llm.adapters.learning-pipeline", havingValue = "zest-eval")
public class ZestEvalLearningPipelineAdapter implements LearningPipelineAdapter {

    private final EvalRunService evalRunService;
    private final AgentProfileProbeService agentProfileProbeService;
    private final AgentProfileResolver agentProfileResolver;
    private final ExecutionSampleService executionSampleService;
    private final LangfuseTraceSampleService langfuseTraceSampleService;
    private final LlmLearningCycleRunRepo learningCycleRunRepo;

    @Override
    public String adapterId() {
        return "zest-eval";
    }

    @Override
    public LearningCycleResult runCycle(LearningCycleRequest request) {
        LearningLoopConfig loop = request.getLearningLoop();
        if (loop == null || !loop.isEnabled()) {
            return LearningCycleResult.builder()
                    .passRate(1.0)
                    .publishAllowed(true)
                    .probePassed(true)
                    .message("learning loop disabled")
                    .build();
        }
        String runCode = "lc-" + UUID.randomUUID().toString().substring(0, 8);
        LlmLearningCycleRunDO row = new LlmLearningCycleRunDO();
        row.setTaskCode(request.getTaskCode());
        row.setProfileVersion(request.getProfileVersion());
        row.setRunCode(runCode);
        row.setStatus("RUNNING");
        row.setStartedAt(LocalDateTime.now());
        learningCycleRunRepo.insert(row);

        String datasetCode = parseDatasetCode(loop.getEvalDatasetRef());
        EvalRunVO evalRun = evalRunService.runDataset(datasetCode);
        double passRate = evalRun.getPassRate() != null
                ? evalRun.getPassRate().doubleValue() / 100.0
                : 0.0;
        boolean evalOk = passRate >= loop.getMinPassRate();

        boolean probePassed = true;
        if (loop.isProbeBeforePublish()) {
            AgentProfileProbeResultVO probe = agentProfileProbeService.probeVersion(
                    request.getTaskCode(), request.getProfileVersion(), new AgentProfileProbeRequest());
            probePassed = probe.isReady();
        }

        boolean publishAllowed = evalOk && probePassed;
        if (loop.isReviewRequired() && !request.isDryRun()) {
            publishAllowed = false;
        }

        List<String> failedCases = new ArrayList<>();
        if (evalRun.getCaseResults() != null) {
            evalRun.getCaseResults().stream()
                    .filter(r -> !Boolean.TRUE.equals(r.get("passed")))
                    .map(r -> String.valueOf(r.get("caseCode")))
                    .forEach(failedCases::add);
        }

        String message = evalOk
                ? (probePassed ? "Eval/Probe passed" : "Probe failed")
                : "Eval passRate " + passRate + " below " + loop.getMinPassRate();

        row.setPassRate(BigDecimal.valueOf(passRate * 100).setScale(2, java.math.RoundingMode.HALF_UP));
        row.setProbePassed(probePassed);
        row.setPublishAllowed(publishAllowed);
        row.setStatus(evalOk && probePassed ? "PASSED" : "FAILED");
        row.setMessage(message);
        row.setFinishedAt(LocalDateTime.now());
        learningCycleRunRepo.updateById(row);

        return LearningCycleResult.builder()
                .passRate(passRate)
                .totalCases(evalRun.getTotalCases())
                .passedCases(evalRun.getPassedCases())
                .probePassed(probePassed)
                .publishAllowed(publishAllowed && evalOk && probePassed)
                .message(message)
                .failedCaseCodes(failedCases)
                .build();
    }

    @Override
    public List<EvalCaseSuggestion> suggestCasesFromTraces(TraceSampleQuery query) {
        List<EvalCaseSuggestion> merged = new ArrayList<>();
        List<String> sources = query.getDistillationSources();
        boolean all = sources == null || sources.isEmpty();
        if (all || sources.contains("execution") || sources.contains("execution:failed")) {
            merged.addAll(executionSampleService.suggestFromExecutions(query));
        }
        if (all || sources.contains("langfuse") || sources.contains("langfuse:low_score")) {
            merged.addAll(langfuseTraceSampleService.suggestFromLangfuse(query));
        }
        int limit = query.getLimit() > 0 ? query.getLimit() : 20;
        if (merged.size() <= limit) {
            return merged;
        }
        return merged.subList(0, limit);
    }

    @Override
    public HealthStatus health() {
        return HealthStatus.builder().up(true).message("zest-eval learning pipeline").build();
    }

    @Override
    public LearningCycleResult validateForPublish(String taskCode, String version, AgentProfileDocument document) {
        LearningLoopConfig loop = ProfileExtensions.learningLoop(document).orElse(null);
        if (loop == null || !loop.isEnabled()) {
            return LearningCycleResult.builder()
                    .passRate(1.0)
                    .publishAllowed(true)
                    .probePassed(true)
                    .message("learning loop disabled")
                    .build();
        }
        String datasetCode = parseDatasetCode(loop.getEvalDatasetRef());
        EvalRunVO evalRun = evalRunService.runDataset(datasetCode);
        double passRate = evalRun.getPassRate() != null
                ? evalRun.getPassRate().doubleValue() / 100.0
                : 0.0;
        boolean evalOk = passRate >= loop.getMinPassRate();
        boolean probePassed = true;
        if (loop.isProbeBeforePublish()) {
            AgentProfileProbeResultVO probe = agentProfileProbeService.probeVersion(
                    taskCode, version, new AgentProfileProbeRequest());
            probePassed = probe.isReady();
        }
        List<String> failedCases = new ArrayList<>();
        if (evalRun.getCaseResults() != null) {
            evalRun.getCaseResults().stream()
                    .filter(r -> !Boolean.TRUE.equals(r.get("passed")))
                    .map(r -> String.valueOf(r.get("caseCode")))
                    .forEach(failedCases::add);
        }
        return LearningCycleResult.builder()
                .passRate(passRate)
                .totalCases(evalRun.getTotalCases())
                .passedCases(evalRun.getPassedCases())
                .probePassed(probePassed)
                .publishAllowed(evalOk && probePassed)
                .message(evalOk ? (probePassed ? "Eval/Probe passed" : "Probe failed")
                        : "Eval below threshold")
                .failedCaseCodes(failedCases)
                .build();
    }

    private String parseDatasetCode(String evalDatasetRef) {
        if (!StringUtils.hasText(evalDatasetRef)) {
            throw new BusinessException("INVALID_PROFILE", "evalDatasetRef is empty");
        }
        int at = evalDatasetRef.indexOf('@');
        return at > 0 ? evalDatasetRef.substring(0, at) : evalDatasetRef;
    }
}
