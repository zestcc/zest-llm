package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("llm_method_registry")
public class LlmMethodRegistryDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long appId;
    private String code;
    private String methodSignature;
    private String inputFields;
    private String outputClass;
    private LocalDateTime registeredAt;
}
