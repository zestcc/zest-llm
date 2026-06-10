package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.vo.CostAlertVO;
import cn.zest.www.zestllm.admin.service.CostAlertQueryService;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cost-alerts")
@RequiredArgsConstructor
public class AdminCostAlertController {

    private final CostAlertQueryService costAlertQueryService;

    @GetMapping
    public Result<List<CostAlertVO>> list(@RequestParam(required = false) String appKey) {
        return Result.success(costAlertQueryService.listRecent(appKey));
    }
}
