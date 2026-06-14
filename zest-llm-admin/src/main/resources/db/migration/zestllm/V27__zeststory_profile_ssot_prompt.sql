-- ZestStory Profile SSOT：Prompt 模板持有任务指令，业务侧仅传占位输入
UPDATE llm_prompt_version
SET template_body = '你是 ZestStory 网文 AI 助手。任务类型：{{taskType}}。
只输出任务要求的内容，禁止元对话与策划腔。

{{#if systemPrompt}}
【作者附加指令】
{{systemPrompt}}

{{/if}}
【业务上下文】
{{userMessage}}'
WHERE id IN (3, 4);
