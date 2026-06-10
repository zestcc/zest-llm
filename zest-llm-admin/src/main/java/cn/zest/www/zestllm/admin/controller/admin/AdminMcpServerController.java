package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.CreateMcpServerRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateMcpServerRequest;
import cn.zest.www.zestllm.admin.model.vo.McpServerVO;
import cn.zest.www.zestllm.admin.service.McpServerManageService;
import cn.zest.www.zestllm.spi.tool.McpToolDescriptor;
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
@RequestMapping("/api/admin/mcp-servers")
@RequiredArgsConstructor
public class AdminMcpServerController {

    private final McpServerManageService mcpServerManageService;

    @GetMapping
    public Result<List<McpServerVO>> list() {
        return Result.success(mcpServerManageService.list());
    }

    @PostMapping
    public Result<McpServerVO> create(@Valid @RequestBody CreateMcpServerRequest request) {
        return Result.success(mcpServerManageService.create(request));
    }

    @PutMapping("/{serverCode}")
    public Result<McpServerVO> update(@PathVariable String serverCode,
                                      @Valid @RequestBody UpdateMcpServerRequest request) {
        return Result.success(mcpServerManageService.update(serverCode, request));
    }

    @GetMapping("/{serverCode}/tools")
    public Result<List<McpToolDescriptor>> listTools(@PathVariable String serverCode) {
        return Result.success(mcpServerManageService.listRemoteTools(serverCode));
    }
}
