package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmMcpServerMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmMcpServerDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmMcpServerRepo {

    private final LlmMcpServerMapper mapper;

    public Optional<LlmMcpServerDO> findByCode(String serverCode) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmMcpServerDO>()
                .eq(LlmMcpServerDO::getServerCode, serverCode)
                .eq(LlmMcpServerDO::getStatus, "ACTIVE")));
    }

    public Optional<LlmMcpServerDO> findByCodeAnyStatus(String serverCode) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmMcpServerDO>()
                .eq(LlmMcpServerDO::getServerCode, serverCode)));
    }

    public List<LlmMcpServerDO> findAllActive() {
        return mapper.selectList(new LambdaQueryWrapper<LlmMcpServerDO>()
                .eq(LlmMcpServerDO::getStatus, "ACTIVE")
                .orderByAsc(LlmMcpServerDO::getServerCode));
    }

    public void insert(LlmMcpServerDO entity) {
        mapper.insert(entity);
    }

    public void update(LlmMcpServerDO entity) {
        mapper.updateById(entity);
    }
}
