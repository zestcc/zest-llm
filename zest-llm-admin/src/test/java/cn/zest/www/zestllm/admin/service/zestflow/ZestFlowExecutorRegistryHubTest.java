package cn.zest.www.zestllm.admin.service.zestflow;

import com.zestflow.common.model.dto.RegisterDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZestFlowExecutorRegistryHubTest {

    @Test
    void registersAndListsExecutor() {
        ZestFlowExecutorRegistryHub hub = new ZestFlowExecutorRegistryHub();
        RegisterDTO dto = RegisterDTO.builder()
                .executorId("demo@127.0.0.1:20551")
                .host("127.0.0.1")
                .port(20551)
                .appCode("zest-llm-demo")
                .appName("zest-llm-demo")
                .build();

        hub.register(dto, 1L);

        assertEquals(1, hub.size());
        assertEquals(1, hub.listOnlinePeers("zest-llm-demo").size());
    }
}
