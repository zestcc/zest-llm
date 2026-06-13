-- SSO 用户联邦字段
ALTER TABLE llm_admin_user
    ADD COLUMN sso_subject VARCHAR(128) DEFAULT NULL COMMENT 'SSO 用户唯一标识' AFTER username,
    ADD COLUMN sso_provider VARCHAR(32) DEFAULT NULL COMMENT 'SSO 提供方' AFTER sso_subject;

ALTER TABLE llm_admin_user
    ADD CONSTRAINT uk_llm_admin_sso_subject UNIQUE (sso_provider, sso_subject);
