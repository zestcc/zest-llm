package cn.zest.www.zestllm.admin.plugin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 插件差异化集成指引（参考 Grafana 数据源详情 / AWS 集成向导）。
 */
@Data
@Builder
public class AdapterPluginGuide {

    /** 卡片与详情页副标题，一句话定位 */
    private String tagline;
    /** 2–4 段概述，说明插件在 Zest Stack 中的角色 */
    private String overview;
    private List<String> useCases;
    private List<String> whenNotToUse;
    /** small | medium | large | all */
    private String recommendedTier;
    /** 数据流 / 调用链文字说明 */
    private String architectureFlow;
    private List<AdapterConfigRef> configRefs;
    private List<AdapterTroubleshootingItem> troubleshooting;
    /** 同 SPI 槽位下的替代插件 catalogKey */
    private List<String> relatedPlugins;
    private List<AdapterDocLink> docLinks;
}
