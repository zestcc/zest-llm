package cn.zest.www.zestllm.admin.flow;

import com.zestflow.executor.chain.ChainDefinition;
import com.zestflow.executor.chain.ChainDefinitionBuilder;
import com.zestflow.executor.chain.ChainManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 启动后向 ZestFlow Executor 注册 production 验收 DAG（内存兜底；Flyway V12 种子由 LlmFlowChainRegistryService 优先加载）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmFlowChainBootstrap implements ApplicationListener<ApplicationReadyEvent> {

    public static final String CHN_AI_CHAT = "CHN_ZESTLLM_AI_CHAT";
    public static final String CHN_INVOKE_AUDIT = "CHN_ZESTLLM_INVOKE_AUDIT";
    public static final String CHN_FLOW_NODE = "CHN_ZESTLLM_FLOW_NODE";
    public static final String CHN_TOOL_LOOP = "CHN_ZESTLLM_TOOL_LOOP";

    private static final String AI_CHAT_CHAIN_JSON = """
            {"code":"CHN_ZESTLLM_AI_CHAT","version":1,"lifecycle":"production","nodes":[{"id":"n1","label":"Invoke","type":"NORMAL","component":"llmFlowInvokeHandler","componentName":"invokeByQuestion"},{"id":"n2","label":"Execution","type":"NORMAL","component":"llmExecutionHandler","componentName":"getExecutionFromInvoke"}],"edges":[{"source":"n1","target":"n2"}]}
            """;

    private static final String INVOKE_AUDIT_CHAIN_JSON = """
            {"code":"CHN_ZESTLLM_INVOKE_AUDIT","version":1,"lifecycle":"production","nodes":[{"id":"n1","label":"Invoke","type":"NORMAL","component":"llmFlowInvokeHandler","componentName":"invokeByQuestion"},{"id":"n2","label":"Execution 审计","type":"NORMAL","component":"llmExecutionHandler","componentName":"getExecutionFromInvoke"}],"edges":[{"source":"n1","target":"n2"}]}
            """;

    private static final String FLOW_NODE_CHAIN_JSON = """
            {"code":"CHN_ZESTLLM_FLOW_NODE","version":1,"lifecycle":"production","nodes":[{"id":"n1","label":"Flow Adapter 节点","type":"NORMAL","component":"zestLlmFlowHandler","componentName":"invokeByCode"}],"edges":[]}
            """;

    private static final String TOOL_LOOP_CHAIN_JSON = """
            {"code":"CHN_ZESTLLM_TOOL_LOOP","version":1,"lifecycle":"production","nodes":[{"id":"n1","label":"MCP Tool Loop","type":"NORMAL","component":"llmFlowInvokeHandler","componentName":"invokeToolLoopChat"}],"edges":[]}
            """;

    private final ChainManager chainManager;
    private final ChainDefinitionBuilder chainDefinitionBuilder;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        registerDemoChains();
    }

    void registerDemoChains() {
        registerIfAbsent(CHN_AI_CHAT, AI_CHAT_CHAIN_JSON);
        registerIfAbsent(CHN_INVOKE_AUDIT, INVOKE_AUDIT_CHAIN_JSON);
        registerIfAbsent(CHN_FLOW_NODE, FLOW_NODE_CHAIN_JSON);
        registerIfAbsent(CHN_TOOL_LOOP, TOOL_LOOP_CHAIN_JSON);
    }

    private void registerIfAbsent(String chainCode, String chainJson) {
        if (chainManager.contains(chainCode)) {
            log.debug("ZestFlow chain already loaded: {}", chainCode);
            return;
        }
        ChainDefinition definition = chainDefinitionBuilder.build(chainCode, 1, chainJson);
        chainManager.load(definition);
        log.info("Registered ZestFlow chain: {}", chainCode);
    }
}
