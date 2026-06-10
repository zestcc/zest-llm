package cn.zest.www.zestllm.spi.profile;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Profile extensions.knowledge — 见 docs/schemas/profile-extensions-v1.1.json
 */
@Data
public class KnowledgeRefConfig {

    private boolean enabled;
    private String provider = "noop";
    private List<String> datasetIds = new ArrayList<>();
    private int topK = 5;
    private double scoreThreshold = 0.6;
    private String injectMode = "system_prefix";
    private String baseUrl;
    private String secretRef;
}
