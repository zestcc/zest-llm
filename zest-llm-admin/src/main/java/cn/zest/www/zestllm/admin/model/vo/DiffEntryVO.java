package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiffEntryVO {
    private String field;
    private String changeType;
    private String before;
    private String after;
    /** 行级 unified diff（`-` 删除 `+` 新增 ` ` 不变） */
    private String unifiedDiff;
}
