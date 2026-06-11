package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.dto.CreateEvalCaseCommand;
import cn.zest.www.zestllm.admin.model.dto.CreateEvalDatasetCommand;
import cn.zest.www.zestllm.admin.model.dto.UpdateEvalCaseCommand;
import cn.zest.www.zestllm.admin.model.entity.LlmEvalCaseDO;
import cn.zest.www.zestllm.admin.model.entity.LlmEvalDatasetDO;
import cn.zest.www.zestllm.admin.model.entity.LlmEvalRunDO;
import cn.zest.www.zestllm.admin.model.vo.EvalCaseVO;
import cn.zest.www.zestllm.admin.model.vo.EvalDatasetVO;
import cn.zest.www.zestllm.admin.model.vo.EvalRunVO;
import cn.zest.www.zestllm.admin.repo.LlmEvalCaseRepo;
import cn.zest.www.zestllm.admin.repo.LlmEvalDatasetRepo;
import cn.zest.www.zestllm.admin.repo.LlmEvalRunRepo;
import cn.zest.www.zestllm.common.api.InvokeRequest;
import cn.zest.www.zestllm.common.api.InvokeResponse;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvalRunService {

    private final LlmEvalDatasetRepo datasetRepo;
    private final LlmEvalCaseRepo caseRepo;
    private final LlmEvalRunRepo runRepo;
    private final LlmInvokeService llmInvokeService;
    private final ObjectMapper objectMapper;

    public List<EvalDatasetVO> listDatasets() {
        return datasetRepo.findAllActive().stream()
                .map(this::toDatasetVo)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public EvalDatasetVO createDataset(CreateEvalDatasetCommand command) {
        if (datasetRepo.findByCode(command.getDatasetCode()).isPresent()) {
            throw new BusinessException("EVAL_DATASET_EXISTS", "数据集已存在: " + command.getDatasetCode());
        }
        LlmEvalDatasetDO row = new LlmEvalDatasetDO();
        row.setDatasetCode(command.getDatasetCode());
        row.setDatasetName(command.getDatasetName());
        row.setAppKey(command.getAppKey());
        row.setTaskCode(command.getTaskCode());
        row.setStatus("ACTIVE");
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        datasetRepo.insert(row);
        return toDatasetVo(row);
    }

    public List<EvalCaseVO> listCases(String datasetCode) {
        LlmEvalDatasetDO dataset = loadDataset(datasetCode);
        return caseRepo.findByDatasetId(dataset.getId()).stream()
                .map(this::toCaseVo)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void createCase(String datasetCode, CreateEvalCaseCommand command) {
        LlmEvalDatasetDO dataset = loadDataset(datasetCode);
        if (command.getCaseCode() == null || command.getCaseCode().isBlank()) {
            throw new BusinessException("EVAL_CASE_INVALID", "用例 code 不能为空");
        }
        if (caseRepo.findByDatasetAndCode(dataset.getId(), command.getCaseCode()).isPresent()) {
            throw new BusinessException("EVAL_CASE_EXISTS", "用例已存在: " + command.getCaseCode());
        }
        LlmEvalCaseDO row = new LlmEvalCaseDO();
        row.setDatasetId(dataset.getId());
        row.setCaseCode(command.getCaseCode().trim());
        row.setInputsJson(toJson(command.getInputs()));
        row.setExpectedJson(toJson(command.getExpected()));
        row.setStatus("ACTIVE");
        row.setCreatedAt(LocalDateTime.now());
        caseRepo.insert(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public EvalCaseVO updateCase(String datasetCode, String caseCode, UpdateEvalCaseCommand command) {
        LlmEvalDatasetDO dataset = loadDataset(datasetCode);
        LlmEvalCaseDO row = loadCase(dataset.getId(), caseCode);
        if (command.getInputs() != null) {
            row.setInputsJson(toJson(command.getInputs()));
        }
        if (command.getExpected() != null) {
            row.setExpectedJson(toJson(command.getExpected()));
        }
        caseRepo.updateById(row);
        return toCaseVo(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCase(String datasetCode, String caseCode) {
        LlmEvalDatasetDO dataset = loadDataset(datasetCode);
        LlmEvalCaseDO row = loadCase(dataset.getId(), caseCode);
        caseRepo.deleteById(row.getId());
    }

    public List<EvalRunVO> listRuns(String datasetCode) {
        LlmEvalDatasetDO dataset = loadDataset(datasetCode);
        return runRepo.findByDatasetId(dataset.getId()).stream()
                .map(row -> toRunVo(row, dataset.getDatasetCode(), null))
                .toList();
    }

    public EvalRunVO getRun(String runCode) {
        LlmEvalRunDO run = runRepo.findByRunCode(runCode)
                .orElseThrow(() -> new BusinessException("EVAL_RUN_NOT_FOUND", "Eval 运行不存在: " + runCode));
        LlmEvalDatasetDO dataset = datasetRepo.findById(run.getDatasetId())
                .orElse(null);
        String datasetCode = dataset != null ? dataset.getDatasetCode() : null;
        List<Map<String, Object>> caseResults = parseReport(run.getReportJson());
        return toRunVo(run, datasetCode, caseResults);
    }

    @Transactional(rollbackFor = Exception.class)
    public EvalRunVO runDataset(String datasetCode) {
        LlmEvalDatasetDO dataset = loadDataset(datasetCode);
        List<LlmEvalCaseDO> cases = caseRepo.findActiveByDatasetId(dataset.getId());
        if (cases.isEmpty()) {
            throw new BusinessException("EVAL_NO_CASES", "数据集无有效用例: " + datasetCode);
        }

        String runCode = "run-" + UUID.randomUUID().toString().substring(0, 8);
        LlmEvalRunDO run = new LlmEvalRunDO();
        run.setDatasetId(dataset.getId());
        run.setRunCode(runCode);
        run.setStatus("RUNNING");
        run.setTotalCases(cases.size());
        run.setPassedCases(0);
        run.setFailedCases(0);
        run.setStartedAt(LocalDateTime.now());
        runRepo.insert(run);

        List<Map<String, Object>> caseResults = new ArrayList<>();
        int passed = 0;
        int failed = 0;

        for (LlmEvalCaseDO evalCase : cases) {
            Map<String, Object> result = runCase(dataset, evalCase);
            caseResults.add(result);
            if (Boolean.TRUE.equals(result.get("passed"))) {
                passed++;
            } else {
                failed++;
            }
        }

        BigDecimal passRate = cases.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(passed * 100.0 / cases.size()).setScale(2, RoundingMode.HALF_UP);

        run.setStatus(failed == 0 ? "PASSED" : "FAILED");
        run.setPassedCases(passed);
        run.setFailedCases(failed);
        run.setPassRate(passRate);
        run.setReportJson(toJson(caseResults));
        run.setFinishedAt(LocalDateTime.now());
        runRepo.updateById(run);

        return toRunVo(run, datasetCode, caseResults);
    }

    private Map<String, Object> runCase(LlmEvalDatasetDO dataset, LlmEvalCaseDO evalCase) {
        Map<String, Object> result = new HashMap<>();
        result.put("caseCode", evalCase.getCaseCode());
        try {
            Map<String, Object> inputs = objectMapper.readValue(
                    evalCase.getInputsJson(), new TypeReference<>() {});
            InvokeRequest request = new InvokeRequest();
            request.setAppKey(dataset.getAppKey());
            request.setCode(dataset.getTaskCode());
            request.setInputs(inputs);
            request.setBizId("eval-" + evalCase.getCaseCode());

            InvokeResponse response = llmInvokeService.invokeForAdmin(request);
            result.put("traceId", response.getTraceId());
            result.put("status", response.getStatus());
            result.put("output", response.getOutput());

            boolean passed = assertExpected(evalCase.getExpectedJson(), response);
            result.put("passed", passed);
            if (!passed) {
                result.put("reason", "expected assertion failed");
            }
        } catch (ZestLlmException ex) {
            result.put("passed", false);
            result.put("status", "FAILED");
            result.put("errorCode", ex.getErrorCode() != null ? ex.getErrorCode().name() : null);
            result.put("errorMessage", ex.getMessage());
            result.put("reason", ex.getMessage());
        } catch (Exception ex) {
            log.warn("Eval case failed caseCode={}", evalCase.getCaseCode(), ex);
            result.put("passed", false);
            result.put("reason", ex.getMessage());
        }
        return result;
    }

    private boolean assertExpected(String expectedJson, InvokeResponse response) {
        if (expectedJson == null || expectedJson.isBlank()) {
            return response.isSuccess();
        }
        try {
            JsonNode expected = objectMapper.readTree(expectedJson);
            if (expected.has("status") && !expected.get("status").asText().equals(response.getStatus())) {
                return false;
            }
            if (expected.has("answerContains")) {
                String needle = expected.get("answerContains").asText();
                String haystack = response.getOutput() != null ? objectMapper.valueToTree(response.getOutput()).toString() : "";
                return haystack.toLowerCase().contains(needle.toLowerCase());
            }
            return response.isSuccess();
        } catch (Exception ex) {
            log.warn("Invalid expected_json for eval case");
            return response.isSuccess();
        }
    }

    private LlmEvalDatasetDO loadDataset(String datasetCode) {
        return datasetRepo.findByCode(datasetCode)
                .orElseThrow(() -> new BusinessException("EVAL_DATASET_NOT_FOUND", "Eval 数据集不存在: " + datasetCode));
    }

    private LlmEvalCaseDO loadCase(Long datasetId, String caseCode) {
        return caseRepo.findByDatasetAndCode(datasetId, caseCode)
                .filter(row -> "ACTIVE".equals(row.getStatus()))
                .orElseThrow(() -> new BusinessException("EVAL_CASE_NOT_FOUND", "Eval 用例不存在: " + caseCode));
    }

    private EvalCaseVO toCaseVo(LlmEvalCaseDO row) {
        return EvalCaseVO.builder()
                .id(row.getId())
                .caseCode(row.getCaseCode())
                .inputs(parseJsonMap(row.getInputsJson()))
                .expected(parseJsonMap(row.getExpectedJson()))
                .status(row.getStatus())
                .createdAt(row.getCreatedAt())
                .build();
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private EvalDatasetVO toDatasetVo(LlmEvalDatasetDO row) {
        return EvalDatasetVO.builder()
                .id(row.getId())
                .datasetCode(row.getDatasetCode())
                .datasetName(row.getDatasetName())
                .appKey(row.getAppKey())
                .taskCode(row.getTaskCode())
                .status(row.getStatus())
                .build();
    }

    private EvalRunVO toRunVo(LlmEvalRunDO row, String datasetCode, List<Map<String, Object>> caseResults) {
        return EvalRunVO.builder()
                .runCode(row.getRunCode())
                .datasetCode(datasetCode)
                .status(row.getStatus())
                .totalCases(row.getTotalCases())
                .passedCases(row.getPassedCases())
                .failedCases(row.getFailedCases())
                .passRate(row.getPassRate())
                .caseResults(caseResults)
                .startedAt(row.getStartedAt())
                .finishedAt(row.getFinishedAt())
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseReport(String reportJson) {
        if (reportJson == null || reportJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(reportJson, new TypeReference<>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[]";
        }
    }
}
