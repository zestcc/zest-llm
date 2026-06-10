package cn.zest.www.zestllm.spi.runtime;

import cn.zest.www.zestllm.spi.model.ChatResponse;
import cn.zest.www.zestllm.spi.model.HealthStatus;

/**
 * 外部 Agent Runtime SPI（Dify / FastGPT 等）。
 * execute 阶段在 runtimeMode=external 时委托本适配器，禁止在实现内修改 Profile 或 publish。
 */
public interface AgentRuntimeAdapter {

    String adapterId();

    ChatResponse invoke(AgentRuntimeInvokeRequest request);

    HealthStatus health();
}
