-- MCP server registry for tool serverRef resolution
CREATE TABLE llm_mcp_server (
    id              BIGINT PRIMARY KEY,
    server_code     VARCHAR(64)  NOT NULL,
    server_name     VARCHAR(128) NOT NULL,
    base_url        VARCHAR(512) NOT NULL,
    auth_secret_ref VARCHAR(256),
    config_json     TEXT,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_mcp_server_code UNIQUE (server_code)
);

INSERT INTO llm_mcp_server (id, server_code, server_name, base_url, auth_secret_ref, config_json, status)
VALUES (1, 'internal-docs', 'Internal Docs MCP', 'http://localhost:9090/mcp', 'env:MCP_SERVER_TOKEN',
        '{"protocol":"jsonrpc","timeoutMs":10000}', 'ACTIVE');
