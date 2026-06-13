package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.CreateGatewayModelRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateGatewayModelRequest;
import cn.zest.www.zestllm.admin.model.vo.GatewayModelVO;
import cn.zest.www.zestllm.admin.service.ModelRegistryManageService;
import com.zestflow.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/gateway-models")
@RequiredArgsConstructor
public class AdminModelRegistryController {

    private final ModelRegistryManageService modelRegistryManageService;

    @GetMapping
    public Result<List<GatewayModelVO>> list() {
        return Result.success(modelRegistryManageService.list());
    }

    @GetMapping("/{modelName}")
    public Result<GatewayModelVO> get(@PathVariable String modelName) {
        return Result.success(modelRegistryManageService.getByModelName(modelName));
    }

    @PostMapping
    public Result<GatewayModelVO> create(@Valid @RequestBody CreateGatewayModelRequest request) {
        return Result.success(modelRegistryManageService.create(request));
    }

    @PutMapping("/{modelName}")
    public Result<GatewayModelVO> update(@PathVariable String modelName,
                                         @Valid @RequestBody UpdateGatewayModelRequest request) {
        return Result.success(modelRegistryManageService.update(modelName, request));
    }
}
