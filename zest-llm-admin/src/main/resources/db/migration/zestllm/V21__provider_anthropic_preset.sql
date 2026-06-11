-- 增加 Anthropic 协议 Provider 预设（与 openai 预设并存，可一键切换）

INSERT INTO llm_provider_preset (id, tenant_id, preset_code, preset_name, provider_type, auth_mode, config_json, sort_order, status)
SELECT 4, NULL, 'litellm-anthropic-local', 'LiteLLM Anthropic 协议', 'litellm', 'API_KEY',
       '{"type":"litellm","baseUrl":"http://localhost:4000","protocol":"anthropic"}', 4, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM llm_provider_preset WHERE preset_code = 'litellm-anthropic-local');
