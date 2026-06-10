package cn.zest.www.zestllm.demo.flow;

import com.zestflow.executor.chain.ChainDefinition;
import com.zestflow.executor.chain.ChainDefinitionBuilder;
import com.zestflow.executor.chain.ChainManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoFlowChainBootstrap implements ApplicationListener<ApplicationReadyEvent> {

    public static final String CHN_DEMO_ORDER_CHAT = "CHN_DEMO_ORDER_CHAT";

    private static final String CHAIN_JSON = """
            {"code":"CHN_DEMO_ORDER_CHAT","version":1,"lifecycle":"production","nodes":[{"id":"n1","label":"Order AI Chat","type":"NORMAL","component":"orderAiFlowHandler","componentName":"chatViaCp"}],"edges":[]}
            """;

    private final ChainManager chainManager;
    private final ChainDefinitionBuilder chainDefinitionBuilder;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (chainManager.contains(CHN_DEMO_ORDER_CHAT)) {
            return;
        }
        ChainDefinition definition = chainDefinitionBuilder.build(CHN_DEMO_ORDER_CHAT, 1, CHAIN_JSON);
        chainManager.load(definition);
        log.info("Registered demo ZestFlow chain: {}", CHN_DEMO_ORDER_CHAT);
    }
}
