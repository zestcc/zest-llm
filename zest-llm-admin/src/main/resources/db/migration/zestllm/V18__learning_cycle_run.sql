-- M3: Learning 闭环周期任务历史
CREATE TABLE IF NOT EXISTS llm_learning_cycle_run (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_code       VARCHAR(64)  NOT NULL,
    profile_version VARCHAR(32)  NOT NULL,
    run_code        VARCHAR(64)  NOT NULL,
    pass_rate       DECIMAL(5,2) NULL,
    probe_passed    TINYINT(1)   NULL,
    publish_allowed TINYINT(1)   NULL,
    status          VARCHAR(32)  NOT NULL,
    message         TEXT         NULL,
    started_at      DATETIME     NULL,
    finished_at     DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_learning_run_code (run_code),
    KEY idx_learning_task (task_code, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
