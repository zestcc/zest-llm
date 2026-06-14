-- ZestStory dogfood：独立 App + AI 作业（与 order-service 解耦）
INSERT INTO llm_app (id, tenant_id, app_key, app_name, token_hash, auth_mode, auth_config_json, status)
VALUES (2, 1, 'zeststory', 'ZestStory', 'e6a154c5c83e1ed2883bc2009cab85f34bdf2487ce9d57b4780966fa641147ae',
        'STATIC_TOKEN', '{"mode":"STATIC_TOKEN"}', 'ACTIVE');

INSERT INTO llm_app_quota (id, app_id, daily_token_limit, qps_limit, daily_cost_limit)
VALUES (2, 2, 5000000, 200, 500.0000);

INSERT INTO llm_auth_binding (id, scope_type, scope_id, inbound_mode, inbound_config_json, outbound_mode, outbound_config_json, status)
VALUES (2, 'APP', 2, 'STATIC_TOKEN', '{"mode":"STATIC_TOKEN"}', 'API_KEY_REF',
        '{"mode":"API_KEY_REF","secretRef":"deepseek-api-key"}', 'ACTIVE');

-- 通用创作 invoke（systemPrompt + userMessage）
INSERT INTO llm_ai_task_def (id, app_id, code, name, description, status)
VALUES (3, 2, 'zestStoryInvoke', 'ZestStory 创作', 'ZestStory 网文创作（system+user 双段提示）', 'ACTIVE');

INSERT INTO llm_prompt_version (id, task_id, version, template_body, output_schema, status, published_at, created_by)
VALUES (3, 3, 'v1',
        '{{systemPrompt}}

{{userMessage}}',
        '{"type":"object","properties":{"answer":{"type":"string"}},"required":["answer"]}',
        'PUBLISHED', CURRENT_TIMESTAMP, 'system');

INSERT INTO llm_model_route (id, task_id, primary_model, fallback_models, max_tokens, temperature, timeout_ms, status)
VALUES (3, 3, 'deepseek-v4-flash', 'deepseek-v4-pro', 4096, 0.70, 120000, 'ACTIVE');

INSERT INTO llm_agent_profile (id, task_id, version, profile_json, provider_preset_code, runtime_mode, status, published_at, created_by)
VALUES (3, 3, 'v1',
'{"apiVersion":"zestllm/v1","runtimeMode":"agent","providerRef":"litellm-local","model":{"primary":"deepseek-v4-flash","fallback":["deepseek-v4-pro"]},"generation":{"maxTokens":4096,"temperature":0.7,"timeoutMs":120000},"extensions":{"runtimeBackend":{"type":"native"},"knowledge":{"enabled":false},"learningLoop":{"enabled":false}},"guardrails":{"piiRedact":false,"blockOnSchemaMismatch":false},"inboundAuth":{"mode":"STATIC_TOKEN"},"outboundAuth":{"mode":"API_KEY_REF","secretRef":"deepseek-api-key"}}',
'litellm-local', 'agent', 'PUBLISHED', CURRENT_TIMESTAMP, 'zeststory-seed');

-- Hybrid RAG（http-knowledge，medium tier）
INSERT INTO llm_ai_task_def (id, app_id, code, name, description, status)
VALUES (4, 2, 'zestStoryRag', 'ZestStory 检索增强', 'ZestStory 设定/章节 RAG 增强创作', 'ACTIVE');

INSERT INTO llm_prompt_version (id, task_id, version, template_body, output_schema, status, published_at, created_by)
VALUES (4, 4, 'v1',
        '{{systemPrompt}}

{{userMessage}}',
        '{"type":"object","properties":{"answer":{"type":"string"}},"required":["answer"]}',
        'PUBLISHED', CURRENT_TIMESTAMP, 'system');

INSERT INTO llm_model_route (id, task_id, primary_model, fallback_models, max_tokens, temperature, timeout_ms, status)
VALUES (4, 4, 'deepseek-v4-flash', 'deepseek-v4-pro', 4096, 0.30, 120000, 'ACTIVE');

INSERT INTO llm_agent_profile (id, task_id, version, profile_json, provider_preset_code, runtime_mode, status, published_at, created_by)
VALUES (4, 4, 'v1',
'{"apiVersion":"zestllm/v1","runtimeMode":"hybrid","providerRef":"litellm-local","model":{"primary":"deepseek-v4-flash"},"generation":{"maxTokens":4096,"temperature":0.3,"timeoutMs":120000},"extensions":{"runtimeBackend":{"type":"native"},"knowledge":{"enabled":true,"provider":"http-knowledge","datasetIds":["zeststory-kb"],"topK":5,"scoreThreshold":0.55,"injectMode":"system_prefix"},"learningLoop":{"enabled":false}},"guardrails":{"blockOnSchemaMismatch":false},"inboundAuth":{"mode":"STATIC_TOKEN"},"outboundAuth":{"mode":"API_KEY_REF","secretRef":"deepseek-api-key"}}',
'litellm-local', 'hybrid', 'PUBLISHED', CURRENT_TIMESTAMP, 'zeststory-seed');
