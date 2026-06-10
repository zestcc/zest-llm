-- Phase3：FinOps 成本告警 + 配额 Webhook

ALTER TABLE llm_app_quota
    ADD COLUMN alert_webhook_url VARCHAR(512) NULL AFTER daily_cost_limit,
    ADD COLUMN alert_threshold_pct INT NOT NULL DEFAULT 80 AFTER alert_webhook_url;

CREATE TABLE llm_cost_alert (
    id              BIGINT PRIMARY KEY,
    app_id          BIGINT       NOT NULL,
    alert_date      DATE         NOT NULL,
    daily_cost      DECIMAL(12, 6),
    cost_limit      DECIMAL(12, 4),
    threshold_pct   INT          NOT NULL DEFAULT 80,
    webhook_url     VARCHAR(512),
    status          VARCHAR(16)  NOT NULL,
    detail_json     TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_cost_alert_app_date UNIQUE (app_id, alert_date),
    CONSTRAINT fk_llm_cost_alert_app FOREIGN KEY (app_id) REFERENCES llm_app (id)
);

UPDATE llm_app_quota
SET alert_webhook_url = 'http://alert-mock:8080/webhook',
    alert_threshold_pct = 80,
    daily_cost_limit = 0.01
WHERE app_id = 1;
