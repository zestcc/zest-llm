package cn.zest.www.zestllm.admin.service;

import cn.zest.www.zestllm.admin.exception.BusinessException;
import cn.zest.www.zestllm.admin.model.entity.LlmAiTaskDefDO;
import cn.zest.www.zestllm.admin.model.entity.LlmPromptVersionDO;
import cn.zest.www.zestllm.admin.model.vo.DiffEntryVO;
import cn.zest.www.zestllm.admin.model.vo.VersionDiffVO;
import cn.zest.www.zestllm.admin.repo.LlmAiTaskDefRepo;
import cn.zest.www.zestllm.admin.repo.LlmPromptVersionRepo;
import cn.zest.www.zestllm.infra.diff.TextDiffUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PromptDiffService {

    private final LlmAiTaskDefRepo taskDefRepo;
    private final LlmPromptVersionRepo promptVersionRepo;

    public VersionDiffVO diff(String taskCode, String fromVersion, String toVersion) {
        LlmAiTaskDefDO task = taskDefRepo.findByCode(taskCode)
                .orElseThrow(() -> new BusinessException("TASK_NOT_FOUND", "AI 作业不存在: " + taskCode));
        LlmPromptVersionDO from = promptVersionRepo.findByTaskIdAndVersion(task.getId(), fromVersion)
                .orElseThrow(() -> new BusinessException("PROMPT_NOT_FOUND", "Prompt 版本不存在: " + fromVersion));
        LlmPromptVersionDO to = promptVersionRepo.findByTaskIdAndVersion(task.getId(), toVersion)
                .orElseThrow(() -> new BusinessException("PROMPT_NOT_FOUND", "Prompt 版本不存在: " + toVersion));

        List<DiffEntryVO> changes = new ArrayList<>();
        addChange(changes, "templateBody", from.getTemplateBody(), to.getTemplateBody());
        addChange(changes, "outputSchema", from.getOutputSchema(), to.getOutputSchema());
        addChange(changes, "status", from.getStatus(), to.getStatus());

        return VersionDiffVO.builder()
                .fromVersion(fromVersion)
                .toVersion(toVersion)
                .changes(changes)
                .build();
    }

    private void addChange(List<DiffEntryVO> changes, String field, String before, String after) {
        if (Objects.equals(before, after)) {
            return;
        }
        DiffEntryVO.DiffEntryVOBuilder builder = DiffEntryVO.builder()
                .field(field)
                .changeType(before == null ? "ADDED" : after == null ? "REMOVED" : "MODIFIED")
                .before(before)
                .after(after);
        if ("templateBody".equals(field)) {
            builder.unifiedDiff(TextDiffUtil.unifiedLineDiff(before, after));
        }
        changes.add(builder.build());
    }
}
