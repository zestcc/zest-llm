-- 修复 Review/Revise Prompt 乱码（UTF-8 正确文案；Flyway 文件须 UTF-8 保存）
UPDATE llm_prompt_version p
JOIN llm_ai_task_def t ON p.task_id = t.id
SET p.template_body = '你是 ZestStory 网文 AI 助手。任务类型：{{taskType}}。
只输出任务要求的内容，禁止元对话与策划腔。

{{#if systemPrompt}}
【作者附加指令】
{{systemPrompt}}

{{/if}}
【业务上下文】
{{userMessage}}',
    p.updated_at = CURRENT_TIMESTAMP
WHERE t.code IN ('zestStoryReview', 'zestStoryRevise')
  AND p.version = 'v1'
  AND p.template_body NOT LIKE '你是 ZestStory%';
