-- 智能体探测 Webhook 告警历史

CREATE TABLE llm_agent_probe_alert (
    id              BIGINT       PRIMARY KEY,
    task_id         BIGINT       NOT NULL,
    task_code       VARCHAR(64)  NOT NULL,
    profile_version VARCHAR(32),
    overall_status  VARCHAR(16)  NOT NULL,
    probe_id        BIGINT,
    webhook_url     VARCHAR(512),
    status          VARCHAR(16)  NOT NULL DEFAULT 'SENT',
    detail_json     TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_llm_agent_probe_alert_task FOREIGN KEY (task_id) REFERENCES llm_ai_task_def (id),
    INDEX idx_agent_probe_alert_task_time (task_code, created_at DESC),
    INDEX idx_agent_probe_alert_status_time (overall_status, created_at DESC)
);
