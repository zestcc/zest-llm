package cn.zest.www.zestllm.starter.registry;

import cn.zest.www.zestllm.common.api.MethodRegistryItem;
import cn.zest.www.zestllm.common.api.MethodRegistryRequest;
import cn.zest.www.zestllm.starter.annotation.AiInput;
import cn.zest.www.zestllm.starter.annotation.ZestLLM;
import cn.zest.www.zestllm.starter.client.LlmControlPlaneClient;
import cn.zest.www.zestllm.starter.config.ZestLlmProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class MethodRegistryScanner implements SmartInitializingSingleton {

    private final ApplicationContext applicationContext;
    private final LlmControlPlaneClient controlPlaneClient;
    private final ZestLlmProperties properties;

    @Override
    public void afterSingletonsInstantiated() {
        if (!properties.isRegistryOnStartup()) {
            log.info("Method registry on startup is disabled");
            return;
        }
        List<MethodRegistryItem> items = scanZestLlmMethods();
        if (items.isEmpty()) {
            log.info("No @ZestLLM methods found to register");
            return;
        }
        MethodRegistryRequest request = new MethodRegistryRequest();
        request.setAppKey(properties.getAppKey());
        request.setMethods(items);
        try {
            controlPlaneClient.registerMethods(request);
            log.info("Registered {} @ZestLLM methods", items.size());
        } catch (Exception e) {
            log.warn("Failed to register @ZestLLM methods on startup: {}", e.getMessage());
        }
    }

    List<MethodRegistryItem> scanZestLlmMethods() {
        List<MethodRegistryItem> items = new ArrayList<>();
        Map<String, Object> beans = applicationContext.getBeansOfType(Object.class);
        for (Object bean : beans.values()) {
            Class<?> targetClass = org.springframework.aop.support.AopUtils.getTargetClass(bean);
            for (Method method : targetClass.getDeclaredMethods()) {
                ZestLLM zestLLM = method.getAnnotation(ZestLLM.class);
                if (zestLLM == null) {
                    continue;
                }
                MethodRegistryItem item = new MethodRegistryItem();
                item.setCode(zestLLM.code());
                item.setMethodSignature(buildSignature(targetClass, method));
                item.setInputFields(extractInputFields(method));
                item.setOutputClass(method.getReturnType().getName());
                items.add(item);
            }
        }
        return items;
    }

    private String buildSignature(Class<?> clazz, Method method) {
        String params = Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
        return clazz.getSimpleName() + "." + method.getName() + "(" + params + ")";
    }

    private List<String> extractInputFields(Method method) {
        List<String> fields = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            AiInput aiInput = parameter.getAnnotation(AiInput.class);
            if (aiInput != null) {
                fields.add(aiInput.value());
            }
        }
        return fields;
    }
}
