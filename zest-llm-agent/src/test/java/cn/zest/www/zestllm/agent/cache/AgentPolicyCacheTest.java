package cn.zest.www.zestllm.agent.cache;

import cn.zest.www.zestllm.common.api.PrepareResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentPolicyCacheTest {

    @Test
    void putAndGet_withinTtl() {
        AgentPolicyCache cache = new AgentPolicyCache(Duration.ofSeconds(60));
        PrepareResponse response = new PrepareResponse();
        response.setTraceId("tr_1");
        response.setCode("aiChat");
        cache.put("order-service", "aiChat", response);

        assertTrue(cache.get("order-service", "aiChat").isPresent());
        assertEquals("tr_1", cache.get("order-service", "aiChat").get().getTraceId());
    }

    @Test
    void get_missWhenExpired() throws InterruptedException {
        AgentPolicyCache cache = new AgentPolicyCache(Duration.ofMillis(50));
        PrepareResponse response = new PrepareResponse();
        response.setCode("aiChat");
        cache.put("order-service", "aiChat", response);

        Thread.sleep(80);
        assertFalse(cache.get("order-service", "aiChat").isPresent());
    }
}
