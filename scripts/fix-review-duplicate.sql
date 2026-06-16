-- 在 zest_llm 库执行：先查重复 Review 任务现状
SELECT t.id, a.app_key, t.code, t.status, t.name
FROM llm_ai_task_def t
JOIN llm_app a ON a.id = t.app_id
WHERE t.code LIKE 'zestStoryReview%'
ORDER BY a.app_key, t.status, t.id;

SELECT 'profile' AS kind, p.task_id, p.version, p.status
FROM llm_agent_profile p
JOIN llm_ai_task_def t ON t.id = p.task_id
WHERE t.code LIKE 'zestStoryReview%';

SELECT 'probe' AS kind, COUNT(*) AS cnt, p.task_id
FROM llm_agent_profile_probe p
JOIN llm_ai_task_def t ON t.id = p.task_id
WHERE t.code LIKE 'zestStoryReview%'
GROUP BY p.task_id;

-- ========== 方式 A：按 task id 删（推荐，先跑上面 SELECT 确认 id）==========
SET @ORPHAN_ID = 2066779401874038785;  -- <<< 改成 INACTIVE / order-service 那条，不是 zeststory ACTIVE

DELETE FROM llm_agent_profile_probe WHERE task_id = @ORPHAN_ID;
DELETE FROM llm_agent_probe_alert WHERE task_id = @ORPHAN_ID;
DELETE FROM llm_agent_profile WHERE task_id = @ORPHAN_ID;
DELETE FROM llm_prompt_version WHERE task_id = @ORPHAN_ID;
DELETE FROM llm_model_route WHERE task_id = @ORPHAN_ID;
DELETE FROM llm_ai_task_def WHERE id = @ORPHAN_ID;

-- ========== 方式 B：按条件批量删孤儿（与方式 A 二选一）==========
/*
DELETE p FROM llm_agent_profile_probe p
JOIN llm_ai_task_def t ON p.task_id = t.id
JOIN llm_app a ON a.id = t.app_id
WHERE t.code = 'zestStoryReview' AND (t.status = 'INACTIVE' OR a.app_key = 'order-service');

DELETE p FROM llm_agent_probe_alert p
JOIN llm_ai_task_def t ON p.task_id = t.id
JOIN llm_app a ON a.id = t.app_id
WHERE t.code = 'zestStoryReview' AND (t.status = 'INACTIVE' OR a.app_key = 'order-service');

DELETE p FROM llm_agent_profile p
JOIN llm_ai_task_def t ON p.task_id = t.id
JOIN llm_app a ON a.id = t.app_id
WHERE t.code = 'zestStoryReview' AND (t.status = 'INACTIVE' OR a.app_key = 'order-service');

DELETE p FROM llm_prompt_version p
JOIN llm_ai_task_def t ON p.task_id = t.id
JOIN llm_app a ON a.id = t.app_id
WHERE t.code = 'zestStoryReview' AND (t.status = 'INACTIVE' OR a.app_key = 'order-service');

DELETE r FROM llm_model_route r
JOIN llm_ai_task_def t ON r.task_id = t.id
JOIN llm_app a ON a.id = t.app_id
WHERE t.code = 'zestStoryReview' AND (t.status = 'INACTIVE' OR a.app_key = 'order-service');

DELETE t FROM llm_ai_task_def t
JOIN llm_app a ON a.id = t.app_id
WHERE t.code = 'zestStoryReview' AND (t.status = 'INACTIVE' OR a.app_key = 'order-service');
*/

-- ========== 若 ACTIVE 的 zeststory Review 仍无 Profile，从 import 配置补一条 ==========
-- （Admin 起来后跑 import 脚本更省事，见下方说明）

-- 验证：只剩一条 ACTIVE + 有 published profile
SELECT t.id, a.app_key, t.code, t.status,
       (SELECT version FROM llm_agent_profile ap WHERE ap.task_id=t.id AND ap.status='PUBLISHED' LIMIT 1) AS published_profile
FROM llm_ai_task_def t
JOIN llm_app a ON a.id = t.app_id
WHERE t.code = 'zestStoryReview';
