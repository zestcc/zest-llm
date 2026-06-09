-- ZestFlow 调度表（MySQL 真源，禁止嵌入式 H2）
-- 与 zestflow-executor V2__init_schedule_schema 对齐

CREATE TABLE IF NOT EXISTS zf_schedule (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    chain_code      VARCHAR(128) NOT NULL,
    chain_name      VARCHAR(128) NOT NULL DEFAULT '',
    cron            VARCHAR(64)  NOT NULL,
    schedule_kind   VARCHAR(16)  NOT NULL DEFAULT 'CRON',
    route_strategy  VARCHAR(32)  NOT NULL DEFAULT 'local',
    shard_total     INT          NOT NULL DEFAULT 1,
    shard_param     VARCHAR(64)  DEFAULT NULL,
    misfire_policy  VARCHAR(16)  NOT NULL DEFAULT 'IGNORE',
    params          TEXT         DEFAULT NULL,
    status          TINYINT      NOT NULL DEFAULT 1,
    remark          VARCHAR(256) DEFAULT NULL,
    tenant_id       BIGINT       NOT NULL DEFAULT 1,
    app_code        VARCHAR(50)  DEFAULT NULL,
    created_by      VARCHAR(64)  DEFAULT NULL,
    updated_by      VARCHAR(64)  DEFAULT NULL,
    created_at      VARCHAR(32)  DEFAULT NULL,
    updated_at      VARCHAR(32)  DEFAULT NULL,
    KEY idx_zf_schedule_status (status),
    KEY idx_zf_schedule_app (tenant_id, app_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS zf_schedule_log (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    schedule_id      BIGINT       NOT NULL,
    chain_code       VARCHAR(128) NOT NULL,
    executor_id      VARCHAR(128) DEFAULT NULL,
    execution_id     VARCHAR(64)  DEFAULT NULL,
    route_strategy   VARCHAR(32)  DEFAULT NULL,
    trigger_type     VARCHAR(32)  NOT NULL DEFAULT 'cron',
    params           TEXT         DEFAULT NULL,
    status           TINYINT      NOT NULL DEFAULT 0,
    error_message    TEXT         DEFAULT NULL,
    cost_ms          BIGINT       DEFAULT NULL,
    triggered_at     VARCHAR(32)  NOT NULL,
    tenant_id        BIGINT       NOT NULL DEFAULT 1,
    app_code         VARCHAR(50)  DEFAULT NULL,
    KEY idx_zf_schedule_log_schedule (schedule_id),
    KEY idx_zf_schedule_log_triggered (triggered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
