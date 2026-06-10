package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmExecutionDO;
import cn.zest.www.zestllm.admin.repo.LlmExecutionRepo;
import cn.zest.www.zestllm.spi.learning.EvalCaseSuggestion;
import cn.zest.www.zestllm.spi.learning.TraceSampleQuery;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExecutionSampleService {

    private final LlmExecutionRepo executionRepo;
    private final ObjectMapper objectMapper;

    public List<EvalCaseSuggestion> suggestFromExecutions(TraceSampleQuery query) {
        int limit = query.getLimit() > 0 ? query.getLimit() : 20;
        LocalDateTime since = query.getSince() != null
                ? LocalDateTime.ofInstant(query.getSince(), java.time.ZoneId.systemDefault())
                : LocalDateTime.now().minusDays(7);

        List<LlmExecutionDO> failed = executionRepo.findRecentByTaskAndStatus(
                query.getTaskCode(), "FAILED", since, limit);
        List<LlmExecutionDO> guardrail = executionRepo.findRecentByTaskAndStatus(
                query.getTaskCode(), "GUARDRAIL_FAIL", since, limit);

        List<EvalCaseSuggestion> suggestions = new ArrayList<>();
        appendSuggestions(suggestions, failed, "execution:failed");
        appendSuggestions(suggestions, guardrail, "execution:guardrail_fail");
        if (suggestions.size() > limit) {
            return suggestions.subList(0, limit);
        }
        return suggestions;
    }

    private void appendSuggestions(List<EvalCaseSuggestion> suggestions, List<LlmExecutionDO> rows, String source) {
        for (LlmExecutionDO row : rows) {
            suggestions.add(EvalCaseSuggestion.builder()
                    .traceId(row.getTraceId())
                    .suggestedInput(parseInput(row.getInputJson()))
                    .suggestedExpected("")
                    .reason(row.getErrorMessage() != null ? row.getErrorMessage() : row.getStatus())
                    .source(source)
                    .build());
        }
    }

    private String parseInput(String inputJson) {
        if (!StringUtils.hasText(inputJson)) {
            return "";
        }
        try {
            Map<String, Object> map = objectMapper.readValue(inputJson, new TypeReference<>() {});
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            return inputJson;
        }
    }
}
