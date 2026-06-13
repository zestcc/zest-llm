package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.CreateSecretRefRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateSecretRefRequest;
import cn.zest.www.zestllm.admin.model.vo.SecretRefVO;
import cn.zest.www.zestllm.admin.service.SecretRefManageService;
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
@RequestMapping("/api/admin/secret-refs")
@RequiredArgsConstructor
public class AdminSecretRefController {

    private final SecretRefManageService secretRefManageService;

    @GetMapping
    public Result<List<SecretRefVO>> list() {
        return Result.success(secretRefManageService.list());
    }

    @GetMapping("/{secretCode}")
    public Result<SecretRefVO> get(@PathVariable String secretCode) {
        return Result.success(secretRefManageService.getByCode(secretCode));
    }

    @PostMapping
    public Result<SecretRefVO> create(@Valid @RequestBody CreateSecretRefRequest request) {
        return Result.success(secretRefManageService.create(request));
    }

    @PutMapping("/{secretCode}")
    public Result<SecretRefVO> update(@PathVariable String secretCode,
                                      @Valid @RequestBody UpdateSecretRefRequest request) {
        return Result.success(secretRefManageService.update(secretCode, request));
    }
}
