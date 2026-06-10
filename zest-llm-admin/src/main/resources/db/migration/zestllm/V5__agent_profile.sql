-- Agent profile, provider preset, auth binding (MySQL 8)
ALTER TABLE llm_app
    ADD COLUMN auth_mode VARCHAR(32) NOT NULL DEFAULT 'STATIC_TOKEN' AFTER token_hash,
    ADD COLUMN auth_config_json TEXT NULL AFTER auth_mode;

CREATE TABLE llm_provider_preset (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NULL,
    preset_code     VARCHAR(64)  NOT NULL,
    preset_name     VARCHAR(128) NOT NULL,
    provider_type   VARCHAR(32)  NOT NULL DEFAULT 'litellm',
    auth_mode       VARCHAR(32)  NOT NULL DEFAULT 'API_KEY',
    config_json     TEXT         NOT NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_provider_preset_code UNIQUE (preset_code),
    CONSTRAINT fk_llm_provider_preset_tenant FOREIGN KEY (tenant_id) REFERENCES llm_tenant (id)
);

CREATE TABLE llm_agent_profile (
    id              BIGINT PRIMARY KEY,
    task_id         BIGINT       NOT NULL,
    version         VARCHAR(32)  NOT NULL,
    profile_json    TEXT         NOT NULL,
    provider_preset_code VARCHAR(64),
    runtime_mode    VARCHAR(16)  NOT NULL DEFAULT 'invoke',
    status          VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    published_at    TIMESTAMP,
    created_by      VARCHAR(64),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_agent_profile_task_version UNIQUE (task_id, version),
    CONSTRAINT fk_llm_agent_profile_task FOREIGN KEY (task_id) REFERENCES llm_ai_task_def (id)
);

CREATE INDEX idx_llm_agent_profile_task_status ON llm_agent_profile (task_id, status);

CREATE TABLE llm_auth_binding (
    id              BIGINT PRIMARY KEY,
    scope_type      VARCHAR(16)  NOT NULL,
    scope_id        BIGINT       NOT NULL,
    inbound_mode    VARCHAR(32)  NOT NULL DEFAULT 'STATIC_TOKEN',
    inbound_config_json TEXT,
    outbound_mode   VARCHAR(32),
    outbound_config_json TEXT,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_auth_binding_scope UNIQUE (scope_type, scope_id)
);
