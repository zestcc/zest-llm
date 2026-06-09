package cn.zest.www.zestllm.admin.controller.registry;

import cn.zest.www.zestllm.admin.service.MethodRegistryService;
import cn.zest.www.zestllm.common.api.MethodRegistryRequest;
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
@RequestMapping("/v1/registry")
@RequiredArgsConstructor
public class MethodRegistryController {

    private final MethodRegistryService methodRegistryService;

    @PostMapping("/methods")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@Valid @RequestBody MethodRegistryRequest request,
                         @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        methodRegistryService.register(extractBearerToken(authorization), request);
    }

    private String extractBearerToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}
