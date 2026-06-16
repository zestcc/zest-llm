-- ZestStory 质量作业：Prompt + ModelRoute + AgentProfile（幂等，按 taskCode 定位）
-- Review / Revise 的 system 指令由 Zestory 经 systemPrompt 占位符注入

INSERT INTO llm_prompt_version (id, task_id, version, template_body, output_schema, status, published_at, created_by)
SELECT 3101, t.id, 'v1',
'你是 ZestStory 网文 AI 助手。任务类型：{{taskType}}。
只输出任务要求的内容，禁止元对话与策划腔。

{{#if systemPrompt}}
【作者附加指令】
{{systemPrompt}}

{{/if}}
【业务上下文】
{{userMessage}}',
'{"type":"object","properties":{"answer":{"type":"string"}},"required":["answer"]}',
'PUBLISHED', CURRENT_TIMESTAMP, 'zeststory-quality-seed'
FROM llm_ai_task_def t
WHERE t.code = 'zestStoryReview'
  AND NOT EXISTS (SELECT 1 FROM llm_prompt_version p WHERE p.task_id = t.id AND p.version = 'v1');

INSERT INTO llm_prompt_version (id, task_id, version, template_body, output_schema, status, published_at, created_by)
SELECT 3102, t.id, 'v1',
'你是 ZestStory 网文 AI 助手。任务类型：{{taskType}}。
只输出任务要求的内容，禁止元对话与策划腔。

{{#if systemPrompt}}
【作者附加指令】
{{systemPrompt}}

{{/if}}
【业务上下文】
{{userMessage}}',
'{"type":"object","properties":{"answer":{"type":"string"}},"required":["answer"]}',
'PUBLISHED', CURRENT_TIMESTAMP, 'zeststory-quality-seed'
FROM llm_ai_task_def t
WHERE t.code = 'zestStoryRevise'
  AND NOT EXISTS (SELECT 1 FROM llm_prompt_version p WHERE p.task_id = t.id AND p.version = 'v1');

INSERT INTO llm_model_route (id, task_id, primary_model, fallback_models, max_tokens, temperature, timeout_ms, status)
SELECT 3201, t.id, 'deepseek-v4-flash', 'deepseek-v4-pro', 1024, 0.25, 120000, 'ACTIVE'
FROM llm_ai_task_def t
WHERE t.code = 'zestStoryReview'
  AND NOT EXISTS (SELECT 1 FROM llm_model_route r WHERE r.task_id = t.id);

INSERT INTO llm_model_route (id, task_id, primary_model, fallback_models, max_tokens, temperature, timeout_ms, status)
SELECT 3202, t.id, 'deepseek-v4-flash', 'deepseek-v4-pro', 4096, 0.35, 120000, 'ACTIVE'
FROM llm_ai_task_def t
WHERE t.code = 'zestStoryRevise'
  AND NOT EXISTS (SELECT 1 FROM llm_model_route r WHERE r.task_id = t.id);

INSERT INTO llm_agent_profile (id, task_id, version, profile_json, provider_preset_code, runtime_mode, status, published_at, created_by)
SELECT 3301, t.id, 'v1',
'{"apiVersion":"zestllm/v1","runtimeMode":"agent","providerRef":"litellm-local","model":{"primary":"deepseek-v4-flash","fallback":["deepseek-v4-pro"]},"generation":{"maxTokens":1024,"temperature":0.25,"timeoutMs":120000},"extensions":{"runtimeBackend":{"type":"native"},"knowledge":{"enabled":false},"learningLoop":{"enabled":false}},"guardrails":{"piiRedact":false,"blockOnSchemaMismatch":false},"inboundAuth":{"mode":"STATIC_TOKEN"},"outboundAuth":{"mode":"API_KEY_REF","secretRef":"deepseek-api-key"}}',
'litellm-local', 'agent', 'DRAFT', NULL, 'zeststory-quality-seed'
FROM llm_ai_task_def t
WHERE t.code = 'zestStoryReview'
  AND NOT EXISTS (SELECT 1 FROM llm_agent_profile p WHERE p.task_id = t.id AND p.version = 'v1');

UPDATE llm_agent_profile p
JOIN llm_ai_task_def t ON p.task_id = t.id
SET p.profile_json = '{"apiVersion":"zestllm/v1","runtimeMode":"agent","providerRef":"litellm-local","model":{"primary":"deepseek-v4-flash","fallback":["deepseek-v4-pro"]},"generation":{"maxTokens":4096,"temperature":0.35,"timeoutMs":120000},"extensions":{"runtimeBackend":{"type":"native"},"knowledge":{"enabled":false},"learningLoop":{"enabled":false}},"guardrails":{"piiRedact":false,"blockOnSchemaMismatch":false},"inboundAuth":{"mode":"STATIC_TOKEN"},"outboundAuth":{"mode":"API_KEY_REF","secretRef":"deepseek-api-key"}}',
    p.provider_preset_code = 'litellm-local',
    p.runtime_mode = 'agent',
    p.updated_at = CURRENT_TIMESTAMP
WHERE t.code = 'zestStoryRevise' AND p.version = 'v1' AND p.status = 'DRAFT';

INSERT INTO llm_agent_profile (id, task_id, version, profile_json, provider_preset_code, runtime_mode, status, published_at, created_by)
SELECT 3302, t.id, 'v1',
'{"apiVersion":"zestllm/v1","runtimeMode":"agent","providerRef":"litellm-local","model":{"primary":"deepseek-v4-flash","fallback":["deepseek-v4-pro"]},"generation":{"maxTokens":4096,"temperature":0.35,"timeoutMs":120000},"extensions":{"runtimeBackend":{"type":"native"},"knowledge":{"enabled":false},"learningLoop":{"enabled":false}},"guardrails":{"piiRedact":false,"blockOnSchemaMismatch":false},"inboundAuth":{"mode":"STATIC_TOKEN"},"outboundAuth":{"mode":"API_KEY_REF","secretRef":"deepseek-api-key"}}',
'litellm-local', 'agent', 'DRAFT', NULL, 'zeststory-quality-seed'
FROM llm_ai_task_def t
WHERE t.code = 'zestStoryRevise'
  AND NOT EXISTS (SELECT 1 FROM llm_agent_profile p WHERE p.task_id = t.id AND p.version = 'v1');
