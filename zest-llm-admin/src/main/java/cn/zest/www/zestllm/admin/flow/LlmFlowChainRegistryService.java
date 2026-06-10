package cn.zest.www.zestllm.admin.flow;

import cn.zest.www.zestllm.admin.model.entity.LlmFlowChainDO;
import cn.zest.www.zestllm.admin.repo.LlmFlowChainRepo;
import com.zestflow.executor.chain.ChainDefinition;
import com.zestflow.executor.chain.ChainDefinitionBuilder;
import com.zestflow.executor.chain.ChainManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class LlmFlowChainRegistryService implements ApplicationListener<ApplicationReadyEvent> {

    private final LlmFlowChainRepo flowChainRepo;
    private final ChainManager chainManager;
    private final ChainDefinitionBuilder chainDefinitionBuilder;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        for (LlmFlowChainDO row : flowChainRepo.findAllActive()) {
            registerFromDb(row);
        }
    }

    private void registerFromDb(LlmFlowChainDO row) {
        if (chainManager.contains(row.getChainCode())) {
            log.debug("ZestFlow chain already registered: {}", row.getChainCode());
            return;
        }
        int version = row.getVersion() != null ? row.getVersion() : 1;
        ChainDefinition definition = chainDefinitionBuilder.build(row.getChainCode(), version, row.getChainData());
        chainManager.load(definition);
        log.info("Loaded ZestFlow chain from DB: {} v{}", row.getChainCode(), version);
    }
}
