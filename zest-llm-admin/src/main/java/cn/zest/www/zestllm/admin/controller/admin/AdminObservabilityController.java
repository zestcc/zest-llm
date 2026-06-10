package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.vo.ObservabilityConfigVO;
import cn.zest.www.zestllm.admin.service.ObservabilityLinkService;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/config/observability")
@RequiredArgsConstructor
public class AdminObservabilityController {

    private final ObservabilityLinkService observabilityLinkService;

    @GetMapping
    public Result<ObservabilityConfigVO> config() {
        return Result.success(observabilityLinkService.config());
    }
}
