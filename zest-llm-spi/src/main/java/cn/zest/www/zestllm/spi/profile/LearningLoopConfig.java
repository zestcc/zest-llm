package cn.zest.www.zestllm.spi.profile;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Profile extensions.learningLoop — 见 docs/schemas/profile-extensions-v1.1.json
 */
@Data
public class LearningLoopConfig {

    private boolean enabled;
    private String evalDatasetRef;
    private double minPassRate = 0.85;
    private boolean probeBeforePublish = true;
    private boolean autoDraftFromFailures;
    private List<String> distillationSources = new ArrayList<>();
    private boolean reviewRequired = true;
}
