package cn.zest.www.zestllm.admin.mapper;

import cn.zest.www.zestllm.admin.model.entity.LlmExecutionDO;
import cn.zest.www.zestllm.admin.model.vo.DailyCostVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LlmExecutionMapper extends BaseMapper<LlmExecutionDO> {

    @Select("SELECT COUNT(*) FROM llm_execution")
    long countAll();

    @Select("SELECT COUNT(*) FROM llm_execution WHERE status = 'SUCCESS'")
    long countSuccess();

    @Select("SELECT COUNT(*) FROM llm_execution WHERE status = 'FAILED'")
    long countFailed();

    @Select("SELECT COALESCE(SUM(cost), 0) FROM llm_execution")
    BigDecimal sumTotalCost();

    @Select("SELECT COUNT(*) FROM llm_execution WHERE created_at >= CURDATE()")
    long countToday();

    @Select("SELECT COUNT(*) FROM llm_execution WHERE task_code = #{taskCode} AND created_at >= #{since}")
    long countByTaskCodeSince(@Param("taskCode") String taskCode, @Param("since") LocalDateTime since);

    @Select("SELECT COUNT(*) FROM llm_execution WHERE task_code = #{taskCode} AND status = #{status} AND created_at >= #{since}")
    long countByTaskCodeAndStatusSince(@Param("taskCode") String taskCode,
                                       @Param("status") String status,
                                       @Param("since") LocalDateTime since);

    @Select("SELECT COUNT(*) FROM llm_execution WHERE app_id = #{appId} AND created_at >= #{since}")
    long countByAppIdSince(@Param("appId") Long appId, @Param("since") LocalDateTime since);

    @Select("SELECT COUNT(*) FROM llm_execution WHERE app_id = #{appId} AND status = #{status} AND created_at >= #{since}")
    long countByAppIdAndStatusSince(@Param("appId") Long appId,
                                    @Param("status") String status,
                                    @Param("since") LocalDateTime since);

    @Select("SELECT COALESCE(SUM(COALESCE(prompt_tokens, 0) + COALESCE(completion_tokens, 0)), 0) FROM llm_execution WHERE app_id = #{appId} AND created_at >= CURDATE()")
    long sumTodayTokensByAppId(Long appId);

    @Select("SELECT COALESCE(SUM(cost), 0) FROM llm_execution WHERE app_id = #{appId} AND created_at >= CURDATE()")
    BigDecimal sumTodayCostByAppId(@Param("appId") Long appId);

    @Select("SELECT COUNT(*) FROM llm_execution_archive")
    long countArchived();

    @Select("""
            SELECT DATE(created_at) AS date,
                   COALESCE(SUM(cost), 0) AS totalCost,
                   COUNT(*) AS callCount,
                   COALESCE(SUM(prompt_tokens), 0) AS promptTokens,
                   COALESCE(SUM(completion_tokens), 0) AS completionTokens
            FROM llm_execution
            WHERE created_at >= #{startAt}
            GROUP BY DATE(created_at)
            ORDER BY date
            """)
    List<DailyCostVO> dailyCostSince(@Param("startAt") LocalDateTime startAt);
}
