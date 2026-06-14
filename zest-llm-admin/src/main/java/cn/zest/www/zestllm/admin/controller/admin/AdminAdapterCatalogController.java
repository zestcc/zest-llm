package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.vo.AdapterCatalogDetailVO;
import cn.zest.www.zestllm.admin.model.vo.AdapterCatalogPageVO;
import cn.zest.www.zestllm.admin.service.AdapterCatalogService;
import cn.zest.www.zestllm.admin.service.AdapterConfigService;
import cn.zest.www.zestllm.admin.service.ExternalAdapterAdminService;
import com.zestflow.common.model.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/adapters/catalog")
@RequiredArgsConstructor
public class AdminAdapterCatalogController {

    private final AdapterCatalogService adapterCatalogService;
    private final AdapterConfigService adapterConfigService;
    private final ExternalAdapterAdminService externalAdapterAdminService;

    @GetMapping
    public Result<AdapterCatalogPageVO> catalog(@RequestParam(required = false) String spiType) {
        return Result.success(adapterCatalogService.catalog(spiType));
    }

    @PostMapping("/external/rescan")
    public Result<Map<String, Object>> rescanExternal() {
        int loaded = externalAdapterAdminService.rescanExternalPlugins();
        return Result.success(Map.of(
                "loaded", loaded,
                "externalPlugins", externalAdapterAdminService.listExternalPlugins(),
                "message", "已刷新外置注册表；新 JAR 仍需重启进程才能加载"
        ));
    }

    @GetMapping("/{catalogKey}")
    public Result<AdapterCatalogDetailVO> detail(@PathVariable String catalogKey) {
        return Result.success(adapterCatalogService.detail(catalogKey));
    }

    @PostMapping("/{catalogKey}/health-check")
    public Result<AdapterCatalogDetailVO> healthCheck(@PathVariable String catalogKey) {
        return Result.success(adapterCatalogService.healthCheck(catalogKey));
    }

    @PutMapping("/defaults/{spiType}")
    public Result<Map<String, String>> setDefault(@PathVariable String spiType,
                                                   @RequestBody DefaultAdapterRequest request) {
        adapterConfigService.setDefaultAdapter(spiType, request.getPluginId());
        return Result.success(adapterConfigService.listOverrides());
    }

    @PostMapping("/reload-config")
    public Result<Map<String, String>> reloadConfig() {
        adapterConfigService.refreshChecker();
        return Result.success(adapterConfigService.listOverrides());
    }

    @Data
    public static class DefaultAdapterRequest {
        private String pluginId;
    }
}
