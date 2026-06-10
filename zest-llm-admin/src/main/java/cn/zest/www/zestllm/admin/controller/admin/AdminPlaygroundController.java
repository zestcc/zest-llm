package cn.zest.www.zestllm.admin.controller.admin;

import cn.zest.www.zestllm.admin.model.dto.PlaygroundPreviewCommand;
import cn.zest.www.zestllm.admin.model.dto.PlaygroundRunCommand;
import cn.zest.www.zestllm.admin.model.vo.PlaygroundPreviewVO;
import cn.zest.www.zestllm.admin.model.vo.PlaygroundRunVO;
import cn.zest.www.zestllm.admin.service.PlaygroundService;
import com.zestflow.common.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/playground")
@RequiredArgsConstructor
public class AdminPlaygroundController {

    private final PlaygroundService playgroundService;

    @PostMapping("/preview")
    public Result<PlaygroundPreviewVO> preview(@RequestBody PlaygroundPreviewCommand command) {
        return Result.success(playgroundService.preview(command));
    }

    @PostMapping("/run")
    public Result<PlaygroundRunVO> run(@RequestBody PlaygroundRunCommand command) {
        return Result.success(playgroundService.run(command));
    }
}
