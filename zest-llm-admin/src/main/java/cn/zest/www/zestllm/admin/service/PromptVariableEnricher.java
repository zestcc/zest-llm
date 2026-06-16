package cn.zest.www.zestllm.admin.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 为 Handlebars Prompt 注入 taskType 布尔开关，避免模板内字符串比较。
 */
public final class PromptVariableEnricher {

    private static final Set<String> KNOWN_TASK_TYPES = Set.of(
            "AUTO_CHAPTER_PROSE",
            "AUTO_CHAPTER",
            "CONTINUE",
            "POLISH",
            "GENERATE",
            "AUTO_CHAPTER_TITLE",
            "AUTO_CHAPTER_SETTING",
            "DISTILL"
    );

    private PromptVariableEnricher() {
    }

    public static Map<String, Object> enrich(Map<String, Object> inputs) {
        Map<String, Object> vars = new LinkedHashMap<>(inputs != null ? inputs : Map.of());
        String taskType = normalizeTaskType(vars.get("taskType"));
        vars.put("taskType", taskType);
        for (String known : KNOWN_TASK_TYPES) {
            vars.put(taskFlagName(known), known.equals(taskType));
        }
        return vars;
    }

    static String normalizeTaskType(Object raw) {
        if (raw == null) {
            return "";
        }
        return String.valueOf(raw).trim().toUpperCase();
    }

    static String taskFlagName(String taskType) {
        return "task" + toCamel(taskType);
    }

    private static String toCamel(String taskType) {
        String[] parts = taskType.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }
}
