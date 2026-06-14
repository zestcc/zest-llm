package cn.zest.www.zestllm.plugin.gateway.litellm;

import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.HttpURLConnection;
import java.util.Map;

/**
 * 鎸夊崗璁负妯″瀷缃戝叧闄勫姞閴存潈澶淬€? * Anthropic/MaaS 閫氬父浣跨敤 x-api-key锛汷penAI/LiteLLM 浣跨敤 Bearer銆? */
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
            // LiteLLM 浠ｇ悊浠嶆帴鍙?Bearer锛汳aaS 绛夊師鐢?Anthropic 缃戝叧涓昏璁?x-api-key
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

