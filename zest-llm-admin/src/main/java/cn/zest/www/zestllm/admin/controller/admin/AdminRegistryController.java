package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.vo.MethodRegistryVO;
import cn.zest.www.zestllm.admin.service.RegistryQueryService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/registry")
@RequiredArgsConstructor
public class AdminRegistryController {

    private final RegistryQueryService registryQueryService;

    @GetMapping("/methods")
    public Result<Page<MethodRegistryVO>> listMethods(
            @RequestParam(required = false) String appKey,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(registryQueryService.pageMethods(page, size, appKey));
    }
}
