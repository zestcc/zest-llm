-- Adapter / Plugin catalog runtime preferences (Admin 控制台启停与默认 SPI 选择)

CREATE TABLE llm_adapter_config (
    id           BIGINT PRIMARY KEY,
    config_key   VARCHAR(128) NOT NULL,
    spi_type     VARCHAR(64)  NOT NULL,
    plugin_id    VARCHAR(64)  NOT NULL,
    enabled      TINYINT      NOT NULL DEFAULT 1,
    config_json  TEXT         NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_adapter_config_key UNIQUE (config_key)
);

CREATE INDEX idx_llm_adapter_config_spi ON llm_adapter_config (spi_type, plugin_id);
