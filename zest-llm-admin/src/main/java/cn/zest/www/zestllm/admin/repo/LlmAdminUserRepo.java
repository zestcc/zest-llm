package cn.zest.www.zestllm.admin.repo;

import cn.zest.www.zestllm.admin.mapper.LlmAdminUserMapper;
import cn.zest.www.zestllm.admin.model.entity.LlmAdminUserDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LlmAdminUserRepo {

    private final LlmAdminUserMapper mapper;

    public long count() {
        return mapper.selectCount(null);
    }

    public List<LlmAdminUserDO> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<LlmAdminUserDO>()
                .orderByAsc(LlmAdminUserDO::getUsername));
    }

    public Optional<LlmAdminUserDO> findByUsername(String username) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmAdminUserDO>()
                .eq(LlmAdminUserDO::getUsername, username)));
    }

    public Optional<LlmAdminUserDO> findBySsoSubject(String provider, String subject) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LlmAdminUserDO>()
                .eq(LlmAdminUserDO::getSsoProvider, provider)
                .eq(LlmAdminUserDO::getSsoSubject, subject)));
    }

    public void insert(LlmAdminUserDO entity) {
        mapper.insert(entity);
    }

    public void update(LlmAdminUserDO entity) {
        mapper.updateById(entity);
    }
}
