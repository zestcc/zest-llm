package cn.zest.www.zestllm.agent;



import cn.zest.www.zestllm.agent.cache.AgentPolicyCache;
import cn.zest.www.zestllm.agent.config.LlmAgentProperties;
import cn.zest.www.zestllm.agent.report.AgentReportRetryQueue;

import cn.zest.www.zestllm.infra.tool.FunctionCallLoop;

import cn.zest.www.zestllm.infra.gateway.SseStreamHandler;

import cn.zest.www.zestllm.infra.tool.ToolOrchestrator;

import cn.zest.www.zestllm.spi.guardrails.ContentModerationAdapter;
import cn.zest.www.zestllm.spi.schema.OutputSchemaValidator;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.AutoConfiguration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.context.annotation.Bean;

import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.http.MediaType;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

import org.springframework.web.client.RestClient;



import java.time.Duration;



@AutoConfiguration

@EnableScheduling

@ConditionalOnProperty(name = "zest.llm.agent.enabled", havingValue = "true")

@EnableConfigurationProperties(LlmAgentProperties.class)

public class LlmAgentAutoConfiguration {



    @Bean

    @ConditionalOnMissingBean

    public ObjectMapper llmAgentObjectMapper() {

        return new ObjectMapper();

    }



    @Bean

    @ConditionalOnMissingBean(name = "llmAgentControlPlaneRestClient")

    public RestClient llmAgentControlPlaneRestClient(LlmAgentProperties properties) {

        return buildRestClient(properties.getControlPlaneUrl(), properties.getAuthToken(), 10, 30);

    }



    @Bean

    @ConditionalOnMissingBean(name = "llmAgentLiteLlmRestClient")

    public RestClient llmAgentLiteLlmRestClient(LlmAgentProperties properties) {

        return buildRestClient(properties.getLitellmUrl(), properties.getLitellmApiKey(), 5, 120);

    }



    @Bean

    @ConditionalOnMissingBean

    public AgentPolicyCache agentPolicyCache(LlmAgentProperties properties) {

        return new AgentPolicyCache(properties.getCacheTtl());

    }



    @Bean

    @ConditionalOnMissingBean

    public AgentReportRetryQueue agentReportRetryQueue(RestClient llmAgentControlPlaneRestClient) {

        return new AgentReportRetryQueue(llmAgentControlPlaneRestClient);

    }



    @Bean

    @ConditionalOnMissingBean

    public LlmAgentClient llmAgentClient(LlmAgentProperties properties,

                                         RestClient llmAgentControlPlaneRestClient,

                                         RestClient llmAgentLiteLlmRestClient,

                                         ObjectMapper objectMapper,

                                         OutputSchemaValidator outputSchemaValidator,

                                         ToolOrchestrator toolOrchestrator,

                                         FunctionCallLoop functionCallLoop,

                                         SseStreamHandler sseStreamHandler,

                                         AgentPolicyCache agentPolicyCache,

                                         ContentModerationAdapter contentModerationAdapter,

                                         AgentReportRetryQueue agentReportRetryQueue) {

        return new LlmAgentClient(properties, llmAgentControlPlaneRestClient,

                llmAgentLiteLlmRestClient, objectMapper, outputSchemaValidator, toolOrchestrator,

                functionCallLoop, sseStreamHandler, agentPolicyCache, contentModerationAdapter,

                agentReportRetryQueue);

    }



    private RestClient buildRestClient(String baseUrl, String authToken, int connectSec, int readSec) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(connectSec));

        factory.setReadTimeout(Duration.ofSeconds(readSec));

        RestClient.Builder builder = RestClient.builder()

                .baseUrl(baseUrl)

                .requestFactory(factory)

                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        if (authToken != null && !authToken.isBlank()) {

            builder.defaultHeader("Authorization", "Bearer " + authToken);

        }

        return builder.build();

    }

}

