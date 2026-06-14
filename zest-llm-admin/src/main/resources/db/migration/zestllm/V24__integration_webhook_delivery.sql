-- Integration Suite: publish webhook delivery log (DLQ after max retries)

CREATE TABLE llm_integration_webhook_delivery (
    id              BIGINT       PRIMARY KEY,
    event_type      VARCHAR(64)  NOT NULL,
    task_code       VARCHAR(64)  NOT NULL,
    profile_version VARCHAR(32)  NULL,
    webhook_url     VARCHAR(512) NOT NULL,
    payload_hash    VARCHAR(64)  NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    attempt_count   INT          NOT NULL DEFAULT 0,
    max_attempts    INT          NOT NULL DEFAULT 1,
    last_error      TEXT         NULL,
    dead_letter     TINYINT(1)   NOT NULL DEFAULT 0,
    detail_json     TEXT         NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_integration_webhook_delivery_time (created_at DESC),
    INDEX idx_integration_webhook_delivery_status (status, dead_letter, created_at DESC),
    INDEX idx_integration_webhook_delivery_task (task_code, created_at DESC)
);
