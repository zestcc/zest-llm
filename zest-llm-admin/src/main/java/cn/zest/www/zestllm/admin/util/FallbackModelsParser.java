package cn.zest.www.zestllm.admin.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
public final class FallbackModelsParser {

    private FallbackModelsParser() {
    }

    public static List<String> parse(String fallbackModels, ObjectMapper objectMapper) {
        if (fallbackModels == null || fallbackModels.isBlank()) {
            return List.of();
        }
        if (fallbackModels.startsWith("[")) {
            try {
                return objectMapper.readValue(fallbackModels, new TypeReference<List<String>>() {});
            } catch (JsonProcessingException ex) {
                log.warn("Invalid fallback_models JSON: {}", fallbackModels);
            }
        }
        return Arrays.stream(fallbackModels.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
