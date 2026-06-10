-- 智能体 Profile 可用性探测历史

CREATE TABLE llm_agent_profile_probe (
    id              BIGINT       PRIMARY KEY,
    task_id         BIGINT       NOT NULL,
    task_code       VARCHAR(64)  NOT NULL,
    profile_version VARCHAR(32),
    profile_status  VARCHAR(16),
    overall_status  VARCHAR(16)  NOT NULL,
    ready           TINYINT(1)   NOT NULL DEFAULT 0,
    smoke_test      TINYINT(1)   NOT NULL DEFAULT 0,
    probe_source    VARCHAR(16)  NOT NULL DEFAULT 'MANUAL',
    latency_ms      BIGINT,
    checks_json     MEDIUMTEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_llm_agent_profile_probe_task FOREIGN KEY (task_id) REFERENCES llm_ai_task_def (id),
    INDEX idx_agent_probe_task_time (task_id, created_at DESC),
    INDEX idx_agent_probe_status_time (overall_status, created_at DESC)
);
