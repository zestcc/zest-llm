-- ZestLLM core schema (MySQL 8)
CREATE TABLE llm_tenant (
    id              BIGINT PRIMARY KEY,
    tenant_code     VARCHAR(64)  NOT NULL,
    tenant_name     VARCHAR(128) NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_tenant_code UNIQUE (tenant_code)
);

CREATE TABLE llm_app (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    app_key         VARCHAR(64)  NOT NULL,
    app_name        VARCHAR(128) NOT NULL,
    token_hash      VARCHAR(128) NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_app_key UNIQUE (app_key),
    CONSTRAINT fk_llm_app_tenant FOREIGN KEY (tenant_id) REFERENCES llm_tenant (id)
);

CREATE TABLE llm_ai_task_def (
    id              BIGINT PRIMARY KEY,
    app_id          BIGINT       NOT NULL,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     TEXT,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_task_app_code UNIQUE (app_id, code),
    CONSTRAINT fk_llm_task_app FOREIGN KEY (app_id) REFERENCES llm_app (id)
);

CREATE TABLE llm_prompt_version (
    id              BIGINT PRIMARY KEY,
    task_id         BIGINT       NOT NULL,
    version         VARCHAR(32)  NOT NULL,
    template_body   TEXT         NOT NULL,
    output_schema   TEXT,
    status          VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    published_at    TIMESTAMP,
    created_by      VARCHAR(64),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_prompt_task_version UNIQUE (task_id, version),
    CONSTRAINT fk_llm_prompt_task FOREIGN KEY (task_id) REFERENCES llm_ai_task_def (id)
);

CREATE TABLE llm_model_route (
    id              BIGINT PRIMARY KEY,
    task_id         BIGINT       NOT NULL,
    primary_model   VARCHAR(128) NOT NULL,
    fallback_models TEXT,
    max_tokens      INT,
    temperature     DECIMAL(5, 2),
    timeout_ms      INT          DEFAULT 30000,
    policy_json     TEXT,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_llm_route_task FOREIGN KEY (task_id) REFERENCES llm_ai_task_def (id)
);

CREATE TABLE llm_execution (
    id                  BIGINT PRIMARY KEY,
    trace_id            VARCHAR(64)  NOT NULL,
    app_id              BIGINT       NOT NULL,
    task_id             BIGINT       NOT NULL,
    task_code           VARCHAR(64)  NOT NULL,
    biz_id              VARCHAR(128),
    prompt_version      VARCHAR(32),
    model               VARCHAR(128),
    status              VARCHAR(16)  NOT NULL,
    input_json          TEXT,
    output_json         TEXT,
    error_code          VARCHAR(64),
    error_message       TEXT,
    latency_ms          BIGINT,
    prompt_tokens       INT,
    completion_tokens   INT,
    cost                DECIMAL(12, 6),
    flow_execution_id   VARCHAR(64),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_execution_trace UNIQUE (trace_id)
);

CREATE INDEX idx_llm_execution_app_created ON llm_execution (app_id, created_at DESC);
CREATE INDEX idx_llm_execution_task_code ON llm_execution (task_code);

CREATE TABLE llm_method_registry (
    id                  BIGINT PRIMARY KEY,
    app_id              BIGINT       NOT NULL,
    code                VARCHAR(64)  NOT NULL,
    method_signature    VARCHAR(512),
    input_fields        TEXT,
    output_class        VARCHAR(256),
    registered_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_method_app_code UNIQUE (app_id, code),
    CONSTRAINT fk_llm_method_app FOREIGN KEY (app_id) REFERENCES llm_app (id)
);

CREATE TABLE llm_app_quota (
    id                  BIGINT PRIMARY KEY,
    app_id              BIGINT       NOT NULL,
    daily_token_limit   BIGINT,
    qps_limit           INT,
    daily_cost_limit    DECIMAL(12, 4),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_app_quota_app UNIQUE (app_id),
    CONSTRAINT fk_llm_quota_app FOREIGN KEY (app_id) REFERENCES llm_app (id)
);
