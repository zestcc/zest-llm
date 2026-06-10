-- Provider presets + published agent profile for demo aiChat
INSERT INTO llm_provider_preset (id, tenant_id, preset_code, preset_name, provider_type, auth_mode, config_json, sort_order, status)
VALUES
(1, NULL, 'litellm-default', 'LiteLLM 默认网关', 'litellm', 'API_KEY',
 '{"type":"litellm","baseUrl":"http://litellm:4000","protocol":"openai"}', 1, 'ACTIVE'),
(2, NULL, 'litellm-local', 'LiteLLM 本地开发', 'litellm', 'API_KEY',
 '{"type":"litellm","baseUrl":"http://localhost:4000","protocol":"openai"}', 2, 'ACTIVE'),
(3, NULL, 'openrouter', 'OpenRouter', 'litellm', 'API_KEY',
 '{"type":"litellm","baseUrl":"https://openrouter.ai/api","protocol":"openai","headers":{"HTTP-Referer":"https://zest.wang"}}', 3, 'ACTIVE');

UPDATE llm_app SET auth_mode = 'STATIC_TOKEN', auth_config_json = '{"mode":"STATIC_TOKEN"}' WHERE app_key = 'order-service';

INSERT INTO llm_agent_profile (id, task_id, version, profile_json, provider_preset_code, runtime_mode, status, published_at, created_by)
VALUES (1, 1, 'v1',
'{"apiVersion":"zestllm/v1","runtimeMode":"agent","providerRef":"litellm-default","model":{"primary":"gpt-4o-mini","fallback":["gpt-3.5-turbo"]},"generation":{"maxTokens":1024,"temperature":0.7,"timeoutMs":30000},"tools":[],"guardrails":{"piiRedact":false,"blockOnSchemaMismatch":true},"inboundAuth":{"mode":"STATIC_TOKEN"},"outboundAuth":{"mode":"API_KEY_REF","secretRef":"env:LITELLM_API_KEY"}}',
'litellm-default', 'agent', 'PUBLISHED', CURRENT_TIMESTAMP, 'system');

INSERT INTO llm_auth_binding (id, scope_type, scope_id, inbound_mode, inbound_config_json, outbound_mode, outbound_config_json, status)
VALUES (1, 'APP', 1, 'STATIC_TOKEN', '{"mode":"STATIC_TOKEN"}', 'API_KEY_REF', '{"mode":"API_KEY_REF","secretRef":"env:LITELLM_API_KEY"}', 'ACTIVE');
