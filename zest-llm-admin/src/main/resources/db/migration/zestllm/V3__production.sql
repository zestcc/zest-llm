-- Production admin user & audit log tables

CREATE TABLE llm_admin_user (
    id              BIGINT PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(128) NOT NULL,
    display_name    VARCHAR(128),
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_admin_user_username UNIQUE (username)
);

CREATE TABLE llm_audit_log (
    id              BIGINT PRIMARY KEY,
    actor           VARCHAR(64)  NOT NULL,
    action          VARCHAR(64)  NOT NULL,
    resource_type   VARCHAR(64)  NOT NULL,
    resource_id     VARCHAR(128),
    detail_json     TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_llm_audit_log_created ON llm_audit_log (created_at DESC);
CREATE INDEX idx_llm_audit_log_resource ON llm_audit_log (resource_type, resource_id);
