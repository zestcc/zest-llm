package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmSecretRefDO;
import cn.zest.www.zestllm.admin.model.request.CreateSecretRefRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateSecretRefRequest;
import cn.zest.www.zestllm.admin.model.vo.SecretRefVO;
import cn.zest.www.zestllm.admin.repo.LlmSecretRefRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SecretRefManageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("ENV", "VAULT", "INLINE");

    private final LlmSecretRefRepo secretRefRepo;
    private final AuditService auditService;

    public List<SecretRefVO> list() {
        return secretRefRepo.findAllActive().stream().map(this::toMaskedVO).toList();
    }

    public SecretRefVO getByCode(String secretCode) {
        return toMaskedVO(requireSecret(secretCode));
    }

    @Transactional(rollbackFor = Exception.class)
    public SecretRefVO create(CreateSecretRefRequest request) {
        if (secretRefRepo.findByCodeAnyStatus(request.getSecretCode()).isPresent()) {
            throw new BusinessException("SECRET_EXISTS", "密钥引用已存在: " + request.getSecretCode());
        }
        validateTypeAndFields(request.getSecretType(), request.getEnvKey(), request.getSecretValue());

        LlmSecretRefDO entity = new LlmSecretRefDO();
        entity.setSecretCode(request.getSecretCode());
        entity.setSecretName(request.getSecretName());
        entity.setSecretType(request.getSecretType().toUpperCase());
        entity.setSecretValue(request.getSecretValue());
        entity.setEnvKey(request.getEnvKey());
        entity.setScopeType(request.getScopeType());
        entity.setScopeId(request.getScopeId());
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        secretRefRepo.insert(entity);
        auditService.log("CREATE", "SECRET_REF", request.getSecretCode(), Map.of());
        return toMaskedVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public SecretRefVO update(String secretCode, UpdateSecretRefRequest request) {
        LlmSecretRefDO entity = requireSecret(secretCode);
        if (StringUtils.hasText(request.getSecretName())) {
            entity.setSecretName(request.getSecretName());
        }
        if (StringUtils.hasText(request.getSecretType())) {
            validateTypeAndFields(request.getSecretType(), request.getEnvKey(), request.getSecretValue());
            entity.setSecretType(request.getSecretType().toUpperCase());
        }
        if (request.getSecretValue() != null) {
            entity.setSecretValue(request.getSecretValue());
        }
        if (request.getEnvKey() != null) {
            entity.setEnvKey(request.getEnvKey());
        }
        if (request.getScopeType() != null) {
            entity.setScopeType(request.getScopeType());
        }
        if (request.getScopeId() != null) {
            entity.setScopeId(request.getScopeId());
        }
        if (StringUtils.hasText(request.getStatus())) {
            entity.setStatus(request.getStatus());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        secretRefRepo.update(entity);
        auditService.log("UPDATE", "SECRET_REF", secretCode, Map.of());
        return toMaskedVO(entity);
    }

    /** Resolve LiteLLM api_key param from secret ref (os.environ/KEY or inline). */
    public String resolveLiteLLMApiKey(String secretCode) {
        if (!StringUtils.hasText(secretCode)) {
            return null;
        }
        LlmSecretRefDO secret = requireSecret(secretCode);
        return switch (secret.getSecretType()) {
            case "ENV" -> StringUtils.hasText(secret.getEnvKey())
                    ? "os.environ/" + secret.getEnvKey() : null;
            case "VAULT" -> StringUtils.hasText(secret.getEnvKey())
                    ? "vault:" + secret.getEnvKey() : null;
            case "INLINE" -> secret.getSecretValue();
            default -> null;
        };
    }

    LlmSecretRefDO requireSecret(String secretCode) {
        return secretRefRepo.findByCode(secretCode)
                .orElseThrow(() -> new BusinessException("SECRET_REF_NOT_FOUND", "密钥引用不存在: " + secretCode));
    }

    private void validateTypeAndFields(String secretType, String envKey, String secretValue) {
        if (!StringUtils.hasText(secretType) || !ALLOWED_TYPES.contains(secretType.toUpperCase())) {
            throw new BusinessException("INVALID_SECRET_TYPE", "secret_type 必须为 ENV|VAULT|INLINE");
        }
        String type = secretType.toUpperCase();
        if ("INLINE".equals(type) && !StringUtils.hasText(secretValue)) {
            throw new BusinessException("SECRET_VALUE_REQUIRED", "INLINE 类型需提供 secret_value");
        }
        if (("ENV".equals(type) || "VAULT".equals(type)) && !StringUtils.hasText(envKey)) {
            throw new BusinessException("ENV_KEY_REQUIRED", type + " 类型需提供 env_key");
        }
    }

    SecretRefVO toMaskedVO(LlmSecretRefDO entity) {
        return SecretRefVO.builder()
                .id(entity.getId())
                .secretCode(entity.getSecretCode())
                .secretName(entity.getSecretName())
                .secretType(entity.getSecretType())
                .secretPreview(maskSecret(entity))
                .envKey(entity.getEnvKey())
                .scopeType(entity.getScopeType())
                .scopeId(entity.getScopeId())
                .status(entity.getStatus())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String maskSecret(LlmSecretRefDO entity) {
        return switch (entity.getSecretType()) {
            case "ENV" -> "env:" + entity.getEnvKey();
            case "VAULT" -> "vault:" + entity.getEnvKey();
            case "INLINE" -> maskInline(entity.getSecretValue());
            default -> "***";
        };
    }

    private String maskInline(String value) {
        if (!StringUtils.hasText(value)) {
            return "****";
        }
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}
