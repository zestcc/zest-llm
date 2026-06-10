package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VersionDiffVO {
    private String fromVersion;
    private String toVersion;
    private List<DiffEntryVO> changes;
}
