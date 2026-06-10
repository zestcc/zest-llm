-- Eval 数据集 + ZestFlow 链注册表（方案 A Phase2）

CREATE TABLE llm_eval_dataset (
    id              BIGINT PRIMARY KEY,
    dataset_code    VARCHAR(64)  NOT NULL,
    dataset_name    VARCHAR(128) NOT NULL,
    app_key         VARCHAR(64)  NOT NULL,
    task_code       VARCHAR(64)  NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_eval_dataset_code UNIQUE (dataset_code)
);

CREATE TABLE llm_eval_case (
    id              BIGINT PRIMARY KEY,
    dataset_id      BIGINT       NOT NULL,
    case_code       VARCHAR(64)  NOT NULL,
    inputs_json     TEXT         NOT NULL,
    expected_json   TEXT,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_eval_case UNIQUE (dataset_id, case_code),
    CONSTRAINT fk_llm_eval_case_dataset FOREIGN KEY (dataset_id) REFERENCES llm_eval_dataset (id)
);

CREATE TABLE llm_eval_run (
    id              BIGINT PRIMARY KEY,
    dataset_id      BIGINT       NOT NULL,
    run_code        VARCHAR(64)  NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    total_cases     INT          NOT NULL DEFAULT 0,
    passed_cases    INT          NOT NULL DEFAULT 0,
    failed_cases    INT          NOT NULL DEFAULT 0,
    pass_rate       DECIMAL(5, 2),
    report_json     TEXT,
    started_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at     TIMESTAMP,
    CONSTRAINT uk_llm_eval_run_code UNIQUE (run_code),
    CONSTRAINT fk_llm_eval_run_dataset FOREIGN KEY (dataset_id) REFERENCES llm_eval_dataset (id)
);

CREATE TABLE llm_flow_chain (
    id              BIGINT PRIMARY KEY,
    chain_code      VARCHAR(128) NOT NULL,
    chain_name      VARCHAR(128) NOT NULL DEFAULT '',
    version         INT          NOT NULL DEFAULT 1,
    lifecycle       VARCHAR(32)  NOT NULL DEFAULT 'production',
    chain_data      TEXT         NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_flow_chain_code UNIQUE (chain_code)
);

INSERT INTO llm_eval_dataset (id, dataset_code, dataset_name, app_key, task_code, status)
VALUES (1, 'demo-aichat', 'Demo AI Chat Eval', 'order-service', 'aiChat', 'ACTIVE');

INSERT INTO llm_eval_case (id, dataset_id, case_code, inputs_json, expected_json, status)
VALUES
(1, 1, 'case-hello', '{"question":"hello"}', '{"status":"SUCCESS","answerContains":"mock"}', 'ACTIVE'),
(2, 1, 'case-ping', '{"question":"ping"}', '{"status":"SUCCESS","answerContains":"mock"}', 'ACTIVE');

INSERT INTO llm_flow_chain (id, chain_code, chain_name, version, lifecycle, chain_data, status)
VALUES
(1, 'CHN_ZESTLLM_AI_CHAT', 'AI Chat invoke→audit', 1, 'production',
 '{"code":"CHN_ZESTLLM_AI_CHAT","version":1,"lifecycle":"production","nodes":[{"id":"n1","label":"Invoke","type":"NORMAL","component":"llmFlowInvokeHandler","componentName":"invokeByQuestion"},{"id":"n2","label":"Execution","type":"NORMAL","component":"llmExecutionHandler","componentName":"getExecutionFromInvoke"}],"edges":[{"source":"n1","target":"n2"}]}',
 'ACTIVE'),
(2, 'CHN_ZESTLLM_INVOKE_AUDIT', 'Invoke audit DAG', 1, 'production',
 '{"code":"CHN_ZESTLLM_INVOKE_AUDIT","version":1,"lifecycle":"production","nodes":[{"id":"n1","label":"Invoke","type":"NORMAL","component":"llmFlowInvokeHandler","componentName":"invokeByQuestion"},{"id":"n2","label":"Execution 审计","type":"NORMAL","component":"llmExecutionHandler","componentName":"getExecutionFromInvoke"}],"edges":[{"source":"n1","target":"n2"}]}',
 'ACTIVE'),
(3, 'CHN_ZESTLLM_FLOW_NODE', 'Flow adapter node', 1, 'production',
 '{"code":"CHN_ZESTLLM_FLOW_NODE","version":1,"lifecycle":"production","nodes":[{"id":"n1","label":"Flow Adapter 节点","type":"NORMAL","component":"zestLlmFlowHandler","componentName":"invokeByCode"}],"edges":[]}',
 'ACTIVE'),
(4, 'CHN_ZESTLLM_TOOL_LOOP', 'MCP Tool Loop', 1, 'production',
 '{"code":"CHN_ZESTLLM_TOOL_LOOP","version":1,"lifecycle":"production","nodes":[{"id":"n1","label":"MCP Tool Loop","type":"NORMAL","component":"llmFlowInvokeHandler","componentName":"invokeToolLoopChat"}],"edges":[]}',
 'ACTIVE');
