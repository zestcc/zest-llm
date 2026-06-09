package cn.zest.www.zestllm.starter.mapper;

import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class AiResultMapper {

    private final ObjectMapper objectMapper;

    public void mapToOutput(Map<String, Object> output, Object targetBean) {
        if (output == null || targetBean == null) {
            return;
        }
        try {
            objectMapper.updateValue(targetBean, output);
        } catch (Exception e) {
            throw new ZestLlmException(LlmErrorCode.OUTPUT_SCHEMA_MISMATCH, null, e.getMessage());
        }
    }
}
