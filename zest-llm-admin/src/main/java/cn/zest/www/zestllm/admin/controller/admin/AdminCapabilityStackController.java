package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.vo.CapabilityStackVO;
import cn.zest.www.zestllm.admin.model.vo.StackTierVO;
import cn.zest.www.zestllm.admin.service.CapabilityStackService;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/capability-stack")
@RequiredArgsConstructor
public class AdminCapabilityStackController {

    private final CapabilityStackService capabilityStackService;

    @GetMapping
    public Result<CapabilityStackVO> overview() {
        return Result.success(capabilityStackService.overview());
    }

    @GetMapping("/tiers/{tierId}")
    public Result<StackTierVO> tier(@PathVariable String tierId) {
        return Result.success(capabilityStackService.getTier(tierId));
    }

    @GetMapping("/export")
    public Result<Map<String, String>> exportCompose(@RequestParam(defaultValue = "small") String tier) {
        return Result.success(capabilityStackService.exportComposeEnv(tier));
    }
}
