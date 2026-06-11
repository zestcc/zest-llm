package cn.zest.www.zestllm.spi.profile;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ModelConfig {
    private String primary;
    private List<String> fallback = new ArrayList<>();
    /** 可选：覆盖 Provider 的 protocol（openai | anthropic） */
    private String apiProtocol;
}
