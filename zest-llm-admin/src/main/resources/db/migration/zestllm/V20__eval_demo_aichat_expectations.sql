-- 本地真实模型：仅断言 SUCCESS，不再要求 answerContains mock

UPDATE llm_eval_case
SET expected_json = '{"status":"SUCCESS"}'
WHERE case_code IN ('case-hello', 'case-ping')
  AND dataset_id = (SELECT id FROM llm_eval_dataset WHERE dataset_code = 'demo-aichat' LIMIT 1);

DELETE FROM llm_eval_case
WHERE case_code = 'ac43-fail'
  AND dataset_id = (SELECT id FROM llm_eval_dataset WHERE dataset_code = 'demo-aichat' LIMIT 1);
