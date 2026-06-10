-- MCP URL 修正（Docker 网络）+ aiChatTools 作业与 Tool Loop Profile

UPDATE llm_mcp_server
SET base_url = 'http://mcp-mock:8080/mcp',
    updated_at = CURRENT_TIMESTAMP
WHERE server_code = 'internal-docs';

INSERT INTO llm_ai_task_def (id, app_id, code, name, description, status)
VALUES (2, 1, 'aiChatTools', 'AI Chat with MCP Tools', 'Tool loop demo with internal-docs MCP', 'ACTIVE');

INSERT INTO llm_prompt_version (id, task_id, version, template_body, output_schema, status, published_at, created_by)
VALUES (2, 2, 'v1',
        'You are a helpful assistant with access to search tools. User question: {{question}}',
        '{"type":"object","properties":{"answer":{"type":"string"}}}',
        'PUBLISHED', CURRENT_TIMESTAMP, 'system');

INSERT INTO llm_model_route (id, task_id, primary_model, fallback_models, max_tokens, temperature, timeout_ms, status)
VALUES (2, 2, 'gpt-4o-mini', 'gpt-3.5-turbo', 1024, 0.70, 60000, 'ACTIVE');

INSERT INTO llm_agent_profile (id, task_id, version, profile_json, provider_preset_code, runtime_mode, status, published_at, created_by)
VALUES (2, 2, 'v1',
'{"apiVersion":"zestllm/v1","runtimeMode":"agent","providerRef":"litellm-default","toolCallMode":"loop","model":{"primary":"gpt-4o-mini","fallback":["gpt-3.5-turbo"]},"generation":{"maxTokens":1024,"temperature":0.7,"timeoutMs":60000,"maxToolSteps":3},"tools":[{"type":"mcp","name":"search","serverRef":"internal-docs","config":{"toolName":"search"}}],"guardrails":{"piiRedact":false,"blockOnSchemaMismatch":true},"inboundAuth":{"mode":"STATIC_TOKEN"},"outboundAuth":{"mode":"API_KEY_REF","secretRef":"env:LITELLM_API_KEY"}}',
'litellm-default', 'agent', 'PUBLISHED', CURRENT_TIMESTAMP, 'system');
