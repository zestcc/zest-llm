package cn.zest.www.zestllm.infra.gateway;

import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.HttpURLConnection;
import java.util.Map;

/**
 * 按协议为模型网关附加鉴权头。
 * Anthropic/MaaS 通常使用 x-api-key；OpenAI/LiteLLM 使用 Bearer。
 */
public final class GatewayAuthApplier {

    private GatewayAuthApplier() {
    }

    public static void applyToRestClient(RestClient.Builder builder, String protocol, String apiKey) {
        applyToRestClient(builder, protocol, apiKey, Map.of());
    }

    public static void applyToRestClient(RestClient.Builder builder,
                                       String protocol,
                                       String apiKey,
                                       Map<String, Object> extraHeaders) {
        applyExtraHeaders(builder, extraHeaders);
        if (!StringUtils.hasText(apiKey)) {
            return;
        }
        if (GatewayApiProtocol.isAnthropic(protocol)) {
            builder.defaultHeader("x-api-key", apiKey);
            builder.defaultHeader("anthropic-version", "2023-06-01");
            // LiteLLM 代理仍接受 Bearer；MaaS 等原生 Anthropic 网关主要认 x-api-key
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        } else {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }
    }

    public static void applyToHttpConnection(HttpURLConnection connection, String protocol, String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return;
        }
        if (GatewayApiProtocol.isAnthropic(protocol)) {
            connection.setRequestProperty("x-api-key", apiKey);
            connection.setRequestProperty("anthropic-version", "2023-06-01");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        } else {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
    }

    private static void applyExtraHeaders(RestClient.Builder builder, Map<String, Object> extraHeaders) {
        if (extraHeaders == null || extraHeaders.isEmpty()) {
            return;
        }
        extraHeaders.forEach((name, value) -> {
            if (StringUtils.hasText(name) && value != null && StringUtils.hasText(value.toString())) {
                builder.defaultHeader(name, value.toString());
            }
        });
    }
}
