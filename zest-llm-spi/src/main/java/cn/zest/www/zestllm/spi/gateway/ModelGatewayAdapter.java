package cn.zest.www.zestllm.spi.gateway;

import cn.zest.www.zestllm.spi.model.ChatRequest;
import cn.zest.www.zestllm.spi.model.ChatResponse;
import cn.zest.www.zestllm.spi.model.HealthStatus;

/**
 * 模型网关 SPI（默认 LiteLLM，可替换 OneAPI / Spring AI）。
 */
public interface ModelGatewayAdapter {

    String adapterId();

    ChatResponse chat(ChatRequest request);

    HealthStatus health();
}
