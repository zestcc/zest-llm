package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.UpdateQuotaRequest;
import cn.zest.www.zestllm.admin.model.vo.QuotaVO;
import cn.zest.www.zestllm.admin.service.QuotaManageService;
import com.zestflow.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/apps/{appKey}/quota")
@RequiredArgsConstructor
public class AdminQuotaController {

    private final QuotaManageService quotaManageService;

    @GetMapping
    public Result<QuotaVO> getQuota(@PathVariable String appKey) {
        return Result.success(quotaManageService.get(appKey));
    }

    @PutMapping
    public Result<QuotaVO> updateQuota(@PathVariable String appKey,
                                       @Valid @RequestBody UpdateQuotaRequest request) {
        return Result.success(quotaManageService.update(appKey, request));
    }
}
