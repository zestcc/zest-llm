package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.model.entity.LlmAppDO;
import cn.zest.www.zestllm.admin.model.vo.AppOverviewVO;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmAppRepo;
import cn.zest.www.zestllm.admin.repo.LlmExecutionRepo;
import cn.zest.www.zestllm.admin.repo.LlmMethodRegistryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppOverviewService {

    private final LlmAppRepo appRepo;
    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmMethodRegistryRepo methodRegistryRepo;
    private final LlmExecutionRepo executionRepo;

    public List<AppOverviewVO> listOverview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<AppOverviewVO> result = new ArrayList<>();
        for (LlmAppDO app : appRepo.findAll()) {
            long taskCount = taskDefRepo.findByAppId(app.getId()).size();
            long methodCount = methodRegistryRepo.pageByAppId(1, 1, app.getId()).getTotal();
            long executionsToday = executionRepo.countByAppIdSince(app.getId(), todayStart);
            long failedToday = executionRepo.countByAppIdAndStatusSince(app.getId(), "FAILED", todayStart);
            result.add(AppOverviewVO.builder()
                    .appKey(app.getAppKey())
                    .appName(app.getAppName())
                    .status(app.getStatus())
                    .taskCount(taskCount)
                    .methodCount(methodCount)
                    .executionsToday(executionsToday)
                    .failedToday(failedToday)
                    .build());
        }
        return result;
    }
}
