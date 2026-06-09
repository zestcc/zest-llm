package cn.zest.www.zestllm.spi.schema;

import java.util.Map;

public interface OutputSchemaValidator {

    String adapterId();

    ValidationResult validate(String schemaJson, Map<String, Object> outputMap);
}
