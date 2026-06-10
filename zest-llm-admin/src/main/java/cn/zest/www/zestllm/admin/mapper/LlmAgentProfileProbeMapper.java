package cn.zest.www.zestllm.admin.mapper;

import cn.zest.www.zestllm.admin.model.entity.LlmAgentProfileProbeDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface LlmAgentProfileProbeMapper extends BaseMapper<LlmAgentProfileProbeDO> {

    @Select("""
            SELECT p.*
            FROM llm_agent_profile_probe p
            INNER JOIN (
                SELECT task_id, MAX(id) AS max_id
                FROM llm_agent_profile_probe
                GROUP BY task_id
            ) latest ON p.id = latest.max_id
            ORDER BY p.created_at DESC
            """)
    List<LlmAgentProfileProbeDO> selectLatestPerTask();

    @Select("""
            SELECT COUNT(*)
            FROM llm_agent_profile_probe p
            INNER JOIN (
                SELECT task_id, MAX(id) AS max_id
                FROM llm_agent_profile_probe
                GROUP BY task_id
            ) latest ON p.id = latest.max_id
            WHERE p.overall_status = #{status}
            """)
    long countLatestByStatus(@Param("status") String status);
}
