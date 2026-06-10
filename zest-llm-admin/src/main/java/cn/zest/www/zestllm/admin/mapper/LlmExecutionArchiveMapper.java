package cn.zest.www.zestllm.admin.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface LlmExecutionArchiveMapper {

    @Insert("""
            INSERT INTO llm_execution_archive (
                id, trace_id, app_id, task_id, task_code, biz_id, prompt_version, model, status,
                input_json, output_json, error_code, error_message, latency_ms, prompt_tokens,
                completion_tokens, cost, flow_execution_id, created_at, archived_at
            )
            SELECT id, trace_id, app_id, task_id, task_code, biz_id, prompt_version, model, status,
                   input_json, output_json, error_code, error_message, latency_ms, prompt_tokens,
                   completion_tokens, cost, flow_execution_id, created_at, CURRENT_TIMESTAMP
            FROM llm_execution
            WHERE created_at < #{cutoff}
            """)
    int archiveBefore(@Param("cutoff") LocalDateTime cutoff);

    @Delete("DELETE FROM llm_execution WHERE created_at < #{cutoff}")
    int deleteBefore(@Param("cutoff") LocalDateTime cutoff);
}
