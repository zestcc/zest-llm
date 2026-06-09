-- Demo seed: tenant + order-service app + aiChat task + prompt v1 + model route
INSERT INTO llm_tenant (id, tenant_code, tenant_name, status)
VALUES (1, 'zest-demo', 'Zest Demo Tenant', 'ACTIVE');

INSERT INTO llm_app (id, tenant_id, app_key, app_name, token_hash, status)
VALUES (1, 1, 'order-service', 'Order Service', '85d35da9c6db00a7d7c61ffbb863b3b94a0b9c6c26f96f3ed4f62ac1b612069f', 'ACTIVE');

INSERT INTO llm_ai_task_def (id, app_id, code, name, description, status)
VALUES (1, 1, 'aiChat', 'AI Chat', 'Demo AI chat task for order service', 'ACTIVE');

INSERT INTO llm_prompt_version (id, task_id, version, template_body, output_schema, status, published_at, created_by)
VALUES (1, 1, 'v1',
        'You are a helpful assistant for order service. User question: {{question}}',
        '{"type":"object","properties":{"answer":{"type":"string"}}}',
        'PUBLISHED', CURRENT_TIMESTAMP, 'system');

INSERT INTO llm_model_route (id, task_id, primary_model, fallback_models, max_tokens, temperature, timeout_ms, status)
VALUES (1, 1, 'gpt-4o-mini', 'gpt-3.5-turbo', 1024, 0.70, 30000, 'ACTIVE');

INSERT INTO llm_app_quota (id, app_id, daily_token_limit, qps_limit, daily_cost_limit)
VALUES (1, 1, 1000000, 100, 100.0000);
