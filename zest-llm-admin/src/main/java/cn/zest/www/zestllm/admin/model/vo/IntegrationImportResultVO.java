package cn.zest.www.zestllm.admin.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IntegrationImportResultVO {
    private int created;
    private int updated;
    private int skipped;
    private List<String> errors;
}
