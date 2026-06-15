-- 恢复 ZestStory 默认 runtime token（SHA-256 of zeststory-runtime-dev-token）
-- 控制面「旋转 Token」后 Zestory 侧需同步；本迁移用于本地联调一键对齐。
UPDATE llm_app
SET token_hash = 'e6a154c5c83e1ed2883bc2009cab85f34bdf2487ce9d57b4780966fa641147ae',
    auth_mode  = 'STATIC_TOKEN',
    status     = 'ACTIVE'
WHERE app_key = 'zeststory';
