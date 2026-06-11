package cn.zest.www.zestllm.infra.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * LiteLLM 流式解析：OpenAI SSE 与 Anthropic Messages SSE。
 */
@Slf4j
@RequiredArgsConstructor
public class SseStreamHandler {

    private final ObjectMapper objectMapper;

    public void streamPost(String baseUrl,
                           String apiKey,
                           String bodyJson,
                           Consumer<String> onDelta,
                           Runnable onComplete) {
        streamPost(baseUrl, apiKey, bodyJson, GatewayApiProtocol.OPENAI, onDelta, onComplete);
    }

    public void streamPost(String baseUrl,
                           String apiKey,
                           String bodyJson,
                           String apiProtocol,
                           Consumer<String> onDelta,
                           Runnable onComplete) {
        if (GatewayApiProtocol.isAnthropic(apiProtocol)) {
            streamAnthropic(baseUrl, apiKey, bodyJson, apiProtocol, onDelta, onComplete);
        } else {
            streamOpenAi(baseUrl, apiKey, bodyJson, onDelta, onComplete);
        }
    }

    private void streamOpenAi(String baseUrl,
                              String apiKey,
                              String bodyJson,
                              Consumer<String> onDelta,
                              Runnable onComplete) {
        HttpURLConnection connection = null;
        try {
            URI uri = URI.create(baseUrl.replaceAll("/$", "") + "/v1/chat/completions");
            connection = openStreamConnection(uri, apiKey, GatewayApiProtocol.OPENAI);
            writeBody(connection, bodyJson);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    JsonNode node = objectMapper.readTree(data);
                    JsonNode deltaNode = node.path("choices").path(0).path("delta").path("content");
                    if (!deltaNode.isMissingNode() && !deltaNode.isNull()) {
                        String delta = deltaNode.asText();
                        if (!delta.isEmpty()) {
                            onDelta.accept(delta);
                        }
                    }
                }
            }
            if (onComplete != null) {
                onComplete.run();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Stream request failed: " + ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void streamAnthropic(String baseUrl,
                                 String apiKey,
                                 String bodyJson,
                                 String apiProtocol,
                                 Consumer<String> onDelta,
                                 Runnable onComplete) {
        HttpURLConnection connection = null;
        try {
            URI uri = URI.create(baseUrl.replaceAll("/$", "") + "/v1/messages");
            connection = openStreamConnection(uri, apiKey, apiProtocol);
            writeBody(connection, bodyJson);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if (data.isEmpty() || "[DONE]".equals(data)) {
                        continue;
                    }
                    JsonNode node = objectMapper.readTree(data);
                    String type = node.path("type").asText("");
                    if ("content_block_delta".equals(type)) {
                        String delta = node.path("delta").path("text").asText("");
                        if (delta.isEmpty()) {
                            delta = node.path("delta").path("partial_json").asText("");
                        }
                        if (!delta.isEmpty()) {
                            onDelta.accept(delta);
                        }
                    }
                }
            }
            if (onComplete != null) {
                onComplete.run();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Anthropic stream request failed: " + ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private HttpURLConnection openStreamConnection(URI uri, String apiKey, String protocol) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "text/event-stream");
        GatewayAuthApplier.applyToHttpConnection(connection, protocol, apiKey);
        return connection;
    }

    private void writeBody(HttpURLConnection connection, String bodyJson) throws Exception {
        try (OutputStream os = connection.getOutputStream()) {
            os.write(bodyJson.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** 将已有文本按块推送（tool loop 完成后模拟流式输出）。 */
    public void emitTextAsStream(String text, Consumer<String> onDelta, Runnable onComplete) {
        if (text == null || text.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        int chunkSize = 32;
        for (int i = 0; i < text.length(); i += chunkSize) {
            onDelta.accept(text.substring(i, Math.min(i + chunkSize, text.length())));
        }
        if (onComplete != null) {
            onComplete.run();
        }
    }
}
