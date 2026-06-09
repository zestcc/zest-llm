package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmAdminUserMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmAdminUserDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmAdminUserRepo {

    private final LlmAdminUserMapper mapper;

    public long count() {
        return mapper.selectCount(null);
    }

    public Optional<LlmAdminUserDO> findByUsername(String username) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmAdminUserDO>()
                .eq(LlmAdminUserDO::getUsername, username)));
    }

    public void insert(LlmAdminUserDO entity) {
        mapper.insert(entity);
    }
}
