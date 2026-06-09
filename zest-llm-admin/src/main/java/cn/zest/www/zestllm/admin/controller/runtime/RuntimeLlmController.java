package cn.zest.www.zestllm.admin.controller.runtime;

import cn.zest.www.zestllm.admin.service.LlmInvokeService;
import cn.zest.www.zestllm.admin.service.LlmPrepareService;
import cn.zest.www.zestllm.admin.service.LlmReportService;
import cn.zest.www.zestllm.common.api.InvokeRequest;
import cn.zest.www.zestllm.common.api.InvokeResponse;
import cn.zest.www.zestllm.common.api.PrepareRequest;
import cn.zest.www.zestllm.common.api.PrepareResponse;
import cn.zest.www.zestllm.common.api.ReportRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/llm")
@RequiredArgsConstructor
public class RuntimeLlmController {

    private final LlmInvokeService llmInvokeService;
    private final LlmPrepareService llmPrepareService;
    private final LlmReportService llmReportService;

    @PostMapping("/invoke")
    public InvokeResponse invoke(@Valid @RequestBody InvokeRequest request,
                                 @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return llmInvokeService.invoke(extractBearerToken(authorization), request);
    }

    @PostMapping("/prepare")
    public PrepareResponse prepare(@Valid @RequestBody PrepareRequest request,
                                   @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return llmPrepareService.prepare(extractBearerToken(authorization), request);
    }

    @PostMapping("/report")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void report(@Valid @RequestBody ReportRequest request,
                       @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        llmReportService.report(extractBearerToken(authorization), request);
    }

    private String extractBearerToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}
