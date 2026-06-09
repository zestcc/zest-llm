package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmExecutionDO;
import cn.zest.www.zestllm.admin.model.vo.ExecutionVO;
import cn.zest.www.zestllm.admin.repo.LlmExecutionRepo;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExecutionQueryService {

    private final LlmExecutionRepo executionRepo;

    public ExecutionVO getByTraceId(String traceId) {
        LlmExecutionDO execution = executionRepo.findByTraceId(traceId)
                .orElseThrow(() -> new ZestLlmException(LlmErrorCode.INTERNAL_ERROR, traceId, "执行记录不存在"));
        return toVO(execution);
    }

    public Page<ExecutionVO> page(int pageNum, int pageSize, String taskCode, String status) {
        Page<LlmExecutionDO> page = executionRepo.page(pageNum, pageSize, taskCode, status);
        Page<ExecutionVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    private ExecutionVO toVO(LlmExecutionDO execution) {
        return ExecutionVO.builder()
                .traceId(execution.getTraceId())
                .taskCode(execution.getTaskCode())
                .bizId(execution.getBizId())
                .promptVersion(execution.getPromptVersion())
                .model(execution.getModel())
                .status(execution.getStatus())
                .inputJson(execution.getInputJson())
                .outputJson(execution.getOutputJson())
                .errorCode(execution.getErrorCode())
                .errorMessage(execution.getErrorMessage())
                .latencyMs(execution.getLatencyMs())
                .promptTokens(execution.getPromptTokens())
                .completionTokens(execution.getCompletionTokens())
                .cost(execution.getCost())
                .createdAt(execution.getCreatedAt())
                .build();
    }
}
