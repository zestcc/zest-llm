-- Execution 归档运行日志

CREATE TABLE llm_execution_archive_run (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    archived_count  INT          NOT NULL DEFAULT 0,
    deleted_count   INT          NOT NULL DEFAULT 0,
    trigger_source  VARCHAR(32)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_llm_execution_archive_run_created ON llm_execution_archive_run (created_at DESC);
