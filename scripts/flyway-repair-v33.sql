-- Flyway V33 失败时，在 Navicat 先执行这一行再重启 Admin：
DELETE FROM flyway_schema_history WHERE version = '33' AND success = 0;
