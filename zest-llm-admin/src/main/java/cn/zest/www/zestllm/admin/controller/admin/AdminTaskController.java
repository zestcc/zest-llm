package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.request.CreateTaskRequest;
import cn.zest.www.zestllm.admin.model.request.UpdateTaskRequest;
import cn.zest.www.zestllm.admin.model.vo.TaskVO;
import cn.zest.www.zestllm.admin.service.TaskManageService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zestflow.common.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tasks")
@RequiredArgsConstructor
public class AdminTaskController {

    private final TaskManageService taskManageService;

    @GetMapping
    public Result<Page<TaskVO>> listTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String appKey) {
        return Result.success(taskManageService.page(page, size, appKey));
    }

    @PostMapping
    public Result<TaskVO> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return Result.success(taskManageService.create(request));
    }

    @PutMapping("/{code}")
    public Result<TaskVO> updateTask(@PathVariable String code,
                                     @Valid @RequestBody UpdateTaskRequest request) {
        return Result.success(taskManageService.update(code, request));
    }
}
