package cn.zest.www.zestllm.admin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("llm_mcp_server")
public class LlmMcpServerDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String serverCode;
    private String serverName;
    private String baseUrl;
    private String authSecretRef;
    private String configJson;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
