package cn.zest.www.zestllm.admin.controller.zestflow;

import cn.zest.www.zestllm.admin.config.ZestFlowAdminProperties;
import com.zestflow.common.constant.RegistryAuthConstants;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Collector 注册占位（本地 demo 不依赖 Collector，返回成功避免 404 噪音）。
 */
@RestController
@RequestMapping("/api/zestflow/registry/collector")
@RequiredArgsConstructor
public class ZestFlowCollectorRegistryController {

    private final ZestFlowAdminProperties adminProperties;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, Object> body,
                                 @RequestHeader(value = RegistryAuthConstants.REGISTRY_TOKEN_HEADER, required = false) String registryToken) {
        assertRegistryToken(registryToken);
        return Result.success();
    }

    @PostMapping("/heartbeat")
    public Result<Void> heartbeat(@RequestBody Map<String, Object> body,
                                  @RequestHeader(value = RegistryAuthConstants.REGISTRY_TOKEN_HEADER, required = false) String registryToken) {
        assertRegistryToken(registryToken);
        return Result.success();
    }

    @DeleteMapping("/{collectorId}")
    public Result<Void> deregister(@PathVariable String collectorId,
                                   @RequestHeader(value = RegistryAuthConstants.REGISTRY_TOKEN_HEADER, required = false) String registryToken) {
        assertRegistryToken(registryToken);
        return Result.success();
    }

    private void assertRegistryToken(String provided) {
        String expected = adminProperties.getRegistryToken();
        if (!StringUtils.hasText(expected)) {
            return;
        }
        if (!expected.equals(provided)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid registry token");
        }
    }
}
