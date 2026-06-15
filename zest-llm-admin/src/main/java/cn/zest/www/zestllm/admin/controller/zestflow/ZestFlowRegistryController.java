package cn.zest.www.zestllm.admin.controller.zestflow;

import cn.zest.www.zestllm.admin.config.ZestFlowAdminProperties;
import cn.zest.www.zestllm.admin.model.vo.PeerExecutorVO;
import cn.zest.www.zestllm.admin.service.zestflow.ZestFlowExecutorRegistryHub;
import com.zestflow.common.constant.RegistryAuthConstants;
import com.zestflow.common.model.Result;
import com.zestflow.common.model.dto.HeartbeatDTO;
import com.zestflow.common.model.dto.RegisterDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * ZestFlow Executor 注册 Hub（与 zestflow-admin RegistryController 路径对齐）。
 */
@RestController
@RequestMapping("/api/zestflow/registry")
@RequiredArgsConstructor
public class ZestFlowRegistryController {

    private final ZestFlowExecutorRegistryHub registryHub;
    private final ZestFlowAdminProperties adminProperties;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterDTO dto,
                                 @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
                                 @RequestHeader(value = RegistryAuthConstants.REGISTRY_TOKEN_HEADER, required = false) String registryToken) {
        assertRegistryToken(registryToken);
        registryHub.register(dto, tenantId);
        return Result.success();
    }

    @PostMapping("/heartbeat")
    public Result<Void> heartbeat(@RequestBody HeartbeatDTO dto,
                                  @RequestHeader(value = RegistryAuthConstants.REGISTRY_TOKEN_HEADER, required = false) String registryToken) {
        assertRegistryToken(registryToken);
        registryHub.heartbeat(dto);
        return Result.success();
    }

    @DeleteMapping("/{executorId}")
    public Result<Void> deregister(@PathVariable String executorId,
                                   @RequestHeader(value = RegistryAuthConstants.REGISTRY_TOKEN_HEADER, required = false) String registryToken) {
        assertRegistryToken(registryToken);
        registryHub.deregister(executorId);
        return Result.success();
    }

    @PutMapping("/{executorId}/status")
    public Result<Void> updateStatus(@PathVariable String executorId,
                                     @RequestParam Integer status,
                                     @RequestHeader(value = RegistryAuthConstants.REGISTRY_TOKEN_HEADER, required = false) String registryToken) {
        assertRegistryToken(registryToken);
        registryHub.updateStatus(executorId, status);
        return Result.success();
    }

    @GetMapping("/peers")
    public Result<List<PeerExecutorVO>> listPeers(@RequestParam(required = false) String appCode) {
        return Result.success(registryHub.listOnlinePeers(appCode));
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
