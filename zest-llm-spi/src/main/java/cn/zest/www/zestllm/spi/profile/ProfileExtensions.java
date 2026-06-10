package cn.zest.www.zestllm.spi.profile;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

/**
 * 从 AgentProfileDocument.extensions 解析整合型 v1.1 字段。
 */
public final class ProfileExtensions {

    public static final String KEY_RUNTIME_BACKEND = "runtimeBackend";
    public static final String KEY_KNOWLEDGE = "knowledge";
    public static final String KEY_LEARNING_LOOP = "learningLoop";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ProfileExtensions() {
    }

    public static Optional<RuntimeBackendConfig> runtimeBackend(AgentProfileDocument profile) {
        return readExtension(profile, KEY_RUNTIME_BACKEND, RuntimeBackendConfig.class);
    }

    public static Optional<KnowledgeRefConfig> knowledge(AgentProfileDocument profile) {
        return readExtension(profile, KEY_KNOWLEDGE, KnowledgeRefConfig.class);
    }

    public static Optional<LearningLoopConfig> learningLoop(AgentProfileDocument profile) {
        return readExtension(profile, KEY_LEARNING_LOOP, LearningLoopConfig.class);
    }

    private static <T> Optional<T> readExtension(AgentProfileDocument profile, String key, Class<T> type) {
        if (profile == null || profile.getExtensions() == null) {
            return Optional.empty();
        }
        Object raw = profile.getExtensions().get(key);
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.of(MAPPER.convertValue(raw, type));
    }

    public static void writeExtension(AgentProfileDocument profile, String key, Object value) {
        if (profile.getExtensions() == null) {
            profile.setExtensions(new java.util.LinkedHashMap<>());
        }
        profile.getExtensions().put(key, MAPPER.convertValue(value, Map.class));
    }
}
