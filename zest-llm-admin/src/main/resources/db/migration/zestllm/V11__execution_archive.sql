-- Execution 冷归档表（热表 llm_execution 保留近 N 天）

CREATE TABLE llm_execution_archive (
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
    created_at          TIMESTAMP    NOT NULL,
    archived_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_execution_archive_trace UNIQUE (trace_id)
);

CREATE INDEX idx_llm_execution_archive_created ON llm_execution_archive (created_at DESC);
CREATE INDEX idx_llm_execution_archive_task ON llm_execution_archive (task_code, created_at DESC);
