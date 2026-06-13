package cn.zest.www.zestllm.admin.service.sso;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSessionRevocationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AdminSessionRevocationService service;

    @Test
    void revokeAndCheck() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey("zest-llm:admin:sso-logout:admin")).thenReturn(true);

        service.revokeByUsername("admin");

        verify(valueOperations).set(eq("zest-llm:admin:sso-logout:admin"), eq("1"), any(Duration.class));
        assertTrue(service.isRevoked("admin"));
    }

    @Test
    void notRevokedByDefault() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        assertFalse(service.isRevoked("user1"));
    }
}
