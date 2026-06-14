-- 本地 LiteLLM mock（config-local.yaml）对齐：gpt-4o-mini + LITELLM master key
UPDATE llm_model_route
SET primary_model = 'gpt-4o-mini', fallback_models = 'gpt-3.5-turbo'
WHERE id IN (3, 4);

UPDATE llm_agent_profile
SET profile_json = REPLACE(REPLACE(REPLACE(profile_json,
    '"primary":"deepseek-v4-flash"', '"primary":"gpt-4o-mini"'),
    '"fallback":["deepseek-v4-pro"]', '"fallback":["gpt-3.5-turbo"]'),
    '"secretRef":"deepseek-api-key"', '"secretRef":"env:LITELLM_API_KEY"')
WHERE id IN (3, 4);
