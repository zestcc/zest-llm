package cn.zest.www.zestllm.admin.controller.runtime;

import cn.zest.www.zestllm.admin.service.AppIntegrationService;
import cn.zest.www.zestllm.common.api.integration.AppIntegrationStatusResponse;
import cn.zest.www.zestllm.common.api.integration.AppTaskAvailabilityResponse;
import cn.zest.www.zestllm.common.api.integration.AppTaskSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 第三方 App 集成查询 API（runtime Bearer token 鉴权）。
 */
@RestController
@RequestMapping("/v1/apps")
@RequiredArgsConstructor
public class RuntimeAppIntegrationController {

    private final AppIntegrationService appIntegrationService;

    @GetMapping("/{appKey}/integration-status")
    public AppIntegrationStatusResponse integrationStatus(@PathVariable String appKey,
                                                          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                                          String authorization) {
        return appIntegrationService.getIntegrationStatus(extractBearerToken(authorization), appKey);
    }

    @GetMapping("/{appKey}/tasks")
    public List<AppTaskSummary> listTasks(@PathVariable String appKey,
                                          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                          String authorization) {
        return appIntegrationService.listTasks(extractBearerToken(authorization), appKey);
    }

    @GetMapping("/{appKey}/tasks/{code}/availability")
    public AppTaskAvailabilityResponse taskAvailability(@PathVariable String appKey,
                                                      @PathVariable String code,
                                                      @RequestParam(defaultValue = "false") boolean smokeTest,
                                                      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                                                      String authorization) {
        return appIntegrationService.getTaskAvailability(extractBearerToken(authorization), appKey, code, smokeTest);
    }

    private String extractBearerToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}
