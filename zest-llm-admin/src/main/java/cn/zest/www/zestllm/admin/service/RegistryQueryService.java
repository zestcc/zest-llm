package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.entity.LlmMethodRegistryDO;
import cn.zest.www.zestllm.admin.model.vo.MethodRegistryVO;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmMethodRegistryRepo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistryQueryService {

    private final LlmAppRepo appRepo;
    private final LlmMethodRegistryRepo methodRegistryRepo;

    public Page<MethodRegistryVO> pageMethods(int pageNum, int pageSize, String appKey) {
        Page<LlmMethodRegistryDO> page;
        if (StringUtils.hasText(appKey)) {
            Long appId = appRepo.findByAppKey(appKey)
                    .orElseThrow(() -> new BusinessException("APP_NOT_FOUND", "应用不存在: " + appKey))
                    .getId();
            page = methodRegistryRepo.pageByAppId(pageNum, pageSize, appId);
        } else {
            page = methodRegistryRepo.pageAll(pageNum, pageSize);
        }
        Map<Long, String> appKeyById = appRepo.findAll().stream()
                .collect(Collectors.toMap(LlmAppDO::getId, LlmAppDO::getAppKey, (a, b) -> a));
        Page<MethodRegistryVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(entity -> toVO(entity, appKeyById.get(entity.getAppId())))
                .toList());
        return result;
    }

    private MethodRegistryVO toVO(LlmMethodRegistryDO entity, String appKey) {
        return MethodRegistryVO.builder()
                .id(entity.getId())
                .appKey(appKey)
                .code(entity.getCode())
                .methodSignature(entity.getMethodSignature())
                .inputFields(entity.getInputFields())
                .outputClass(entity.getOutputClass())
                .registeredAt(entity.getRegisteredAt())
                .build();
    }
}
