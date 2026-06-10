-- Admin RBAC: role column (ADMIN | OPERATOR)
ALTER TABLE llm_admin_user
    ADD COLUMN role VARCHAR(32) NOT NULL DEFAULT 'ADMIN' COMMENT 'ADMIN|OPERATOR' AFTER status;

UPDATE llm_admin_user SET role = 'ADMIN' WHERE role IS NULL OR role = '';
