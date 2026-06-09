package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmExecutionDO;
import cn.zest.www.zestllm.admin.model.entity.LlmMethodRegistryDO;
import cn.zest.www.zestllm.admin.repo.LlmMethodRegistryRepo;
import cn.zest.www.zestllm.common.api.MethodRegistryItem;
import cn.zest.www.zestllm.common.api.MethodRegistryRequest;
import cn.zest.www.zestllm.common.error.LlmErrorCode;
import cn.zest.www.zestllm.common.error.ZestLlmException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MethodRegistryService {

    private final AppAuthService appAuthService;
    private final LlmMethodRegistryRepo methodRegistryRepo;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public void register(String bearerToken, MethodRegistryRequest request) {
        if (request.getAppKey() == null || request.getMethods() == null) {
            throw new ZestLlmException(LlmErrorCode.INTERNAL_ERROR);
        }
        LlmAppDO app = appAuthService.authenticate(request.getAppKey(), bearerToken);
        for (MethodRegistryItem item : request.getMethods()) {
            upsertMethod(app.getId(), item);
        }
    }

    private void upsertMethod(Long appId, MethodRegistryItem item) {
        LlmMethodRegistryDO existing = methodRegistryRepo.findByAppIdAndCode(appId, item.getCode()).orElse(null);
        String inputFieldsJson = toJson(item.getInputFields());
        if (existing == null) {
            LlmMethodRegistryDO entity = new LlmMethodRegistryDO();
            entity.setAppId(appId);
            entity.setCode(item.getCode());
            entity.setMethodSignature(item.getMethodSignature());
            entity.setInputFields(inputFieldsJson);
            entity.setOutputClass(item.getOutputClass());
            entity.setRegisteredAt(LocalDateTime.now());
            methodRegistryRepo.insert(entity);
        } else {
            existing.setMethodSignature(item.getMethodSignature());
            existing.setInputFields(inputFieldsJson);
            existing.setOutputClass(item.getOutputClass());
            existing.setRegisteredAt(LocalDateTime.now());
            methodRegistryRepo.update(existing);
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ZestLlmException(LlmErrorCode.INTERNAL_ERROR);
        }
    }
}
