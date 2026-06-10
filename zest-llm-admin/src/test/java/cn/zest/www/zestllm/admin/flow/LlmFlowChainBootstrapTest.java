package cn.zest.www.zestllm.admin.flow;

import com.zestflow.executor.chain.ChainDefinition;
import com.zestflow.executor.chain.ChainDefinitionBuilder;
import com.zestflow.executor.chain.ChainManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmFlowChainBootstrapTest {

    @Mock
    private ChainManager chainManager;

    @Mock
    private ChainDefinitionBuilder chainDefinitionBuilder;

    @Mock
    private ChainDefinition chainDefinition;

    @InjectMocks
    private LlmFlowChainBootstrap bootstrap;

    @Test
    void registersDemoChainsOnStartup() {
        when(chainManager.contains(anyString())).thenReturn(false);
        when(chainDefinitionBuilder.build(anyString(), anyInt(), anyString())).thenReturn(chainDefinition);

        bootstrap.registerDemoChains();

        verify(chainDefinitionBuilder, org.mockito.Mockito.times(4)).build(anyString(), eq(1), anyString());
        verify(chainManager, org.mockito.Mockito.times(4)).load(chainDefinition);
    }

    @Test
    void skipsAlreadyLoadedChains() {
        when(chainManager.contains(anyString())).thenReturn(true);

        bootstrap.registerDemoChains();

        verify(chainDefinitionBuilder, never()).build(anyString(), anyInt(), anyString());
        verify(chainManager, never()).load(any());
    }
}
