package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.CreateProviderPresetRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateProviderPresetRequest;
import cn.zest.www.zestllm.admin.model.vo.ProviderPresetVO;
import cn.zest.www.zestllm.admin.service.ProviderPresetManageService;
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
@RequestMapping("/api/admin/provider-presets")
@RequiredArgsConstructor
public class AdminProviderPresetController {

    private final ProviderPresetManageService providerPresetManageService;

    @GetMapping
    public Result<List<ProviderPresetVO>> list() {
        return Result.success(providerPresetManageService.list());
    }

    @PostMapping
    public Result<ProviderPresetVO> create(@Valid @RequestBody CreateProviderPresetRequest request) {
        return Result.success(providerPresetManageService.create(request));
    }

    @PutMapping("/{presetCode}")
    public Result<ProviderPresetVO> update(@PathVariable String presetCode,
                                             @Valid @RequestBody UpdateProviderPresetRequest request) {
        return Result.success(providerPresetManageService.update(presetCode, request));
    }
}
