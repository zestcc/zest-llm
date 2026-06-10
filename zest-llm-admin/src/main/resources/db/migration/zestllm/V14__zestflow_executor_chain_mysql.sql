-- ZestFlow Executor 链表（与 zestflow-executor V1 + V3 对齐）
-- 本地/合一部署复用 spring.datasource，ChainDeclarationSync 依赖 zf_chain 等表

CREATE TABLE IF NOT EXISTS zf_chain (
    code                 VARCHAR(64)  NOT NULL PRIMARY KEY,
    chain_key            VARCHAR(128) DEFAULT NULL COMMENT '应用侧稳定链标识',
    name                 VARCHAR(128) NOT NULL DEFAULT '',
    description          VARCHAR(500) DEFAULT NULL,
    status               TINYINT      NOT NULL DEFAULT 1 COMMENT '0-停用 1-未设置 2-未发布 3-发布中 4-已发布',
    version              INT          NOT NULL DEFAULT 1 COMMENT '当前发布版本号',
    delivery_lifecycle   VARCHAR(16)  NOT NULL DEFAULT 'bootstrap' COMMENT 'bootstrap-占位 production-生产',
    created_by           VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    updated_by           VARCHAR(64)  DEFAULT NULL COMMENT '最后修改人',
    tenant_id            BIGINT       DEFAULT 1 COMMENT '租户ID',
    app_code             VARCHAR(50)  DEFAULT NULL COMMENT '应用编码',
    is_deleted           TINYINT      DEFAULT 0 COMMENT '删除标记：0-未删 1-已删',
    created_at           VARCHAR(32)  DEFAULT NULL COMMENT '创建时间',
    updated_at           VARCHAR(32)  DEFAULT NULL COMMENT '更新时间',
    UNIQUE KEY uk_app_chain_key (tenant_id, app_code, chain_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Executor 链定义';

CREATE TABLE IF NOT EXISTS zf_design (
    code        VARCHAR(64)  NOT NULL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL DEFAULT '',
    description VARCHAR(500) DEFAULT NULL,
    designer    VARCHAR(64)  DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1-启用 0-停用',
    graph_data  TEXT         DEFAULT NULL,
    chain_data  TEXT         DEFAULT NULL COMMENT '翻译后的链定义 JSON',
    created_by  VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    updated_by  VARCHAR(64)  DEFAULT NULL COMMENT '最后修改人',
    tenant_id   BIGINT       DEFAULT 1 COMMENT '租户ID',
    app_code    VARCHAR(50)  DEFAULT NULL COMMENT '应用编码',
    is_deleted  TINYINT      DEFAULT 0 COMMENT '删除标记：0-未删 1-已删',
    created_at  VARCHAR(32)  DEFAULT NULL COMMENT '创建时间',
    updated_at  VARCHAR(32)  DEFAULT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Executor 设计定义';

CREATE TABLE IF NOT EXISTS zf_design_binding (
    design_code VARCHAR(64) NOT NULL,
    chain_code  VARCHAR(64) NOT NULL,
    tenant_id   BIGINT      DEFAULT 1 COMMENT '租户ID',
    app_code    VARCHAR(50) DEFAULT NULL COMMENT '应用编码',
    PRIMARY KEY (design_code, chain_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设计与链绑定关系';

CREATE TABLE IF NOT EXISTS zf_chain_version (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    chain_code  VARCHAR(64)  NOT NULL COMMENT '链编码',
    version     INT          NOT NULL COMMENT '版本号',
    design_code VARCHAR(64)  DEFAULT NULL COMMENT '关联设计编码',
    graph_data  MEDIUMTEXT   DEFAULT NULL COMMENT '图数据 JSON 快照',
    chain_data  MEDIUMTEXT   DEFAULT NULL COMMENT '链定义 JSON 快照',
    created_by  VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    tenant_id   BIGINT       DEFAULT 1 COMMENT '租户ID',
    app_code    VARCHAR(50)  DEFAULT NULL COMMENT '应用编码',
    created_at  VARCHAR(32)  NOT NULL COMMENT '创建时间',
    KEY idx_chain_version (chain_code, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链版本快照表';
