package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.vo.FlowChainVO;
import cn.zest.www.zestllm.admin.service.FlowChainManageService;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/flow-chains")
@RequiredArgsConstructor
public class AdminFlowChainController {

    private final FlowChainManageService flowChainManageService;

    @GetMapping
    public Result<List<FlowChainVO>> list() {
        return Result.success(flowChainManageService.listActive());
    }

    @GetMapping("/{chainCode}")
    public Result<FlowChainVO> get(@PathVariable String chainCode) {
        return Result.success(flowChainManageService.getByCode(chainCode));
    }
}
