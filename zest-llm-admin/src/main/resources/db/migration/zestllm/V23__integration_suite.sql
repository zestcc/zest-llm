-- Integration Suite v1: gateway model SSOT + secret refs (MySQL 8)

CREATE TABLE llm_secret_ref (
    id              BIGINT PRIMARY KEY,
    secret_code     VARCHAR(64)  NOT NULL,
    secret_name     VARCHAR(128) NOT NULL,
    secret_type     VARCHAR(16)  NOT NULL DEFAULT 'ENV',
    secret_value    TEXT         NULL,
    env_key         VARCHAR(128) NULL,
    scope_type      VARCHAR(16)  NULL,
    scope_id        BIGINT       NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_secret_ref_code UNIQUE (secret_code)
);

CREATE TABLE llm_gateway_model (
    id                  BIGINT PRIMARY KEY,
    model_name          VARCHAR(128) NOT NULL,
    upstream_model      VARCHAR(256) NOT NULL,
    api_base            VARCHAR(512) NULL,
    api_key_secret_ref  VARCHAR(64)  NULL,
    extra_json          TEXT         NULL,
    status              VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    sync_status         VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    last_sync_at        TIMESTAMP    NULL,
    sort_order          INT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_gateway_model_name UNIQUE (model_name)
);

CREATE INDEX idx_llm_gateway_model_sync ON llm_gateway_model (sync_status, status);

-- Seed secret refs for LiteLLM config.yaml models
INSERT INTO llm_secret_ref (id, secret_code, secret_name, secret_type, env_key, status, created_at, updated_at)
VALUES
    (230001, 'deepseek-api-key', 'DeepSeek API Key', 'ENV', 'DEEPSEEK_API_KEY', 'ACTIVE', NOW(), NOW());

-- Seed gateway models matching deploy/litellm/config.yaml deepseek-v4-flash/pro
INSERT INTO llm_gateway_model (id, model_name, upstream_model, api_key_secret_ref, status, sync_status, sort_order, created_at, updated_at)
VALUES
    (230101, 'deepseek-v4-flash', 'deepseek/deepseek-v4-flash', 'deepseek-api-key', 'ACTIVE', 'PENDING', 10, NOW(), NOW()),
    (230102, 'deepseek-v4-pro', 'deepseek/deepseek-v4-pro', 'deepseek-api-key', 'ACTIVE', 'PENDING', 20, NOW(), NOW());
