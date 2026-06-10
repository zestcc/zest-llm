package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class CapabilityStackVO {
    private String currentTier;
    private String deployCommand;
    private List<StackTierVO> tiers;
    private List<AdapterHealthVO> adapters;
    private Map<String, String> recommendedAdapters;
}
