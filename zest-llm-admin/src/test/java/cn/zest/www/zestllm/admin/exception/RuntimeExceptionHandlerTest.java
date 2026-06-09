package cn.zest.www.zestllm.admin.exception;

import cn.zest.www.zestllm.common.api.InvokeResponse;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RuntimeExceptionHandlerTest {

    private final RuntimeExceptionHandler handler = new RuntimeExceptionHandler();

    @Test
    void mapsAuthFailedToInvokeResponse() {
        ZestLlmException ex = new ZestLlmException(LlmErrorCode.AUTH_FAILED, "tr_test", "鉴权失败");
        ResponseEntity<?> entity = handler.handleZestLlmException(ex, null);

        assertNotNull(entity.getBody());
        InvokeResponse response = (InvokeResponse) entity.getBody();
        assertEquals("FAILED", response.getStatus());
        assertEquals("AUTH_FAILED", response.getErrorCode());
        assertEquals("tr_test", response.getTraceId());
    }
}
