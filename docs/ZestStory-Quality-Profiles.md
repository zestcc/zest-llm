# ZestStory 质量作业 Profile（zestStoryReview / zestStoryRevise）

Review 与 Revise **必须使用不同的 Prompt**（角色、输出契约不同）。

## 分工

| 层级 | Review | Revise |
|------|--------|--------|
| **ZestLLM Prompt（固定）** | 资深责编 · 四维评审 · **只输出 JSON** | 资深责编 · 修订原则 · **只输出正文** |
| **Zestory `systemPrompt`** | 本书风格、学习规则（`global_quality_learned` 等） | 同上 |
| **Zestory `userMessage`** | 书名、预检、待评审正文 | keepArc、blocking、待修订正文 |

`profileOwnedPrompts: true` 时，任务级指令必须在 **ZestLLM 各作业的 Prompt** 里配置，不能两个作业共用「通用 AI 助手」壳。

## Prompt 文件

| 作业 | POST JSON |
|------|-----------|
| `zestStoryReview` | `deploy/examples/zeststory-review-prompt.post.json` |
| `zestStoryRevise` | `deploy/examples/zeststory-revise-prompt.post.json` |

## 导入 / 升级 Prompt

```powershell
cd D:\WORK\Project\zestLLM

# 首次或升级针对性 Prompt（会 publish 新大版本如 v6）
.\scripts\import-zestllm-quality-profiles.ps1 -FixPrompts

# 全量（Prompt + Route + Profile；已发布 Profile 会跳过更新）
.\scripts\import-zestllm-quality-profiles.ps1 -Publish
```

若报 `400 PROFILE_PUBLISHED`：说明 Profile 已发布，脚本会跳过 PUT；请 **重启 ZestLLM Admin** 以应用 `V33` 重复任务合并与 `findByCode` ACTIVE 优先逻辑，再重跑 `-Publish`。

若 Review 在 Zestory 控制中心 **版本仍为空**：检查 ZestLLM 是否存在两条 `zestStoryReview`（ACTIVE + INACTIVE），执行 V33 或手动删除 INACTIVE 行。

## 验收

Admin → Prompt 管理：

- **Review** 预览含「评审维度」「只输出严格 JSON」
- **Revise** 预览含「修订原则」「只输出修订后的完整正文」
- 两者 **不应** 完全相同
