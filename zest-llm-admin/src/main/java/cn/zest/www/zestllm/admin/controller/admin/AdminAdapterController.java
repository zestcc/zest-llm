package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.vo.AdapterHealthVO;
import cn.zest.www.zestllm.admin.service.AdapterHealthService;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/adapters")
@RequiredArgsConstructor
public class AdminAdapterController {

    private final AdapterHealthService adapterHealthService;

    @GetMapping("/health")
    public Result<AdapterHealthVO> health() {
        return Result.success(adapterHealthService.gatewayHealth());
    }

    @GetMapping("/health/all")
    public Result<List<AdapterHealthVO>> healthAll() {
        return Result.success(adapterHealthService.listAll());
    }
}
