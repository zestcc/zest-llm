package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.infra.config.LangfuseProperties;
import cn.zest.www.zestllm.spi.learning.EvalCaseSuggestion;
import cn.zest.www.zestllm.spi.learning.TraceSampleQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LangfuseTraceSampleServiceTest {

    @Test
    void suggest_whenDisabled_returnsEmpty() {
        LangfuseProperties props = new LangfuseProperties();
        props.setEnabled(false);
        LangfuseTraceSampleService service = new LangfuseTraceSampleService(props, new ObjectMapper());
        List<EvalCaseSuggestion> rows = service.suggestFromLangfuse(TraceSampleQuery.builder()
                .taskCode("aiChat")
                .limit(5)
                .build());
        assertTrue(rows.isEmpty());
    }
}
