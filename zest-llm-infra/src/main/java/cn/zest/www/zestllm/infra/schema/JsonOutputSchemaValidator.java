package cn.zest.www.zestllm.infra.schema;

import cn.zest.www.zestllm.spi.schema.OutputSchemaValidator;
import cn.zest.www.zestllm.spi.schema.ValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class JsonOutputSchemaValidator implements OutputSchemaValidator {

    private final ObjectMapper objectMapper;

    @Override
    public String adapterId() {
        return "json";
    }

    @Override
    public ValidationResult validate(String schemaJson, Map<String, Object> outputMap) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return ValidationResult.builder().valid(true).build();
        }
        if (outputMap == null) {
            return ValidationResult.builder()
                    .valid(false)
                    .message("output is null")
                    .errors(List.of("output is null"))
                    .build();
        }
        try {
            JsonNode schema = objectMapper.readTree(schemaJson);
            List<String> errors = new ArrayList<>();
            validateNode(schema, outputMap, "", errors);
            if (errors.isEmpty()) {
                return ValidationResult.builder().valid(true).build();
            }
            return ValidationResult.builder()
                    .valid(false)
                    .message("output schema mismatch")
                    .errors(errors)
                    .build();
        } catch (Exception ex) {
            log.warn("Failed to validate output schema", ex);
            return ValidationResult.builder()
                    .valid(false)
                    .message(ex.getMessage())
                    .errors(List.of(ex.getMessage()))
                    .build();
        }
    }

    private void validateNode(JsonNode schema, Object value, String path, List<String> errors) {
        String type = schema.path("type").asText("");
        if ("object".equals(type) && value instanceof Map<?, ?> mapValue) {
            JsonNode required = schema.path("required");
            if (required.isArray()) {
                for (JsonNode field : required) {
                    String name = field.asText();
                    if (!mapValue.containsKey(name) || mapValue.get(name) == null) {
                        errors.add(path + "/" + name + " is required");
                    }
                }
            }
            JsonNode properties = schema.path("properties");
            if (properties.isObject()) {
                Iterator<String> names = properties.fieldNames();
                while (names.hasNext()) {
                    String name = names.next();
                    if (mapValue.containsKey(name)) {
                        validateNode(properties.get(name), mapValue.get(name), path + "/" + name, errors);
                    }
                }
            }
            return;
        }
        if (!type.isEmpty() && !matchesType(type, value)) {
            errors.add(path + " expected type " + type + " but was " + describeValue(value));
        }
    }

    private boolean matchesType(String type, Object value) {
        return switch (type) {
            case "string" -> value instanceof String;
            case "integer" -> value instanceof Integer || value instanceof Long;
            case "number" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "array" -> value instanceof List;
            case "object" -> value instanceof Map;
            default -> true;
        };
    }

    private String describeValue(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }
}
