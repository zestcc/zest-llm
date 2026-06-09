# ZestFlow 交付门禁（alwaysApply）

> 与 `.zestflow/rules/architecture.md` 同源；功能开发**完成前必须**走 MCP 交付管道。

## 禁止宣称完成，除非

- 已调用 `validate_delivery(strictMode=true)` 且 `passed=true`
- `usable_score ≥ 0.95` 且 `blocking` 为空

## 标准管道（不可跳过）

```text
search_patterns → plan_chain → scaffold_component → compose_chain
→ validate_chain → gen_smoke_suite → run_acceptance_suite → validate_delivery
→ gen_playground_scene → record_learning_event
```

## 硬约束

- 禁止只写 Handler 无链/设计图
- 禁止单节点 Seeder 冒充 production 交付
- 禁止单体 `@ZestExecute` >80 行不拆分
- bootstrap 链仅 dev 占位；功能交付须 `compose_chain` + `lifecycle=production`

## MCP 工具

| 工具 | 时机 |
|------|------|
| `compose_chain` | 组 production 多节点链 |
| `validate_delivery` | 交付前最后一道门 |
| `gen_smoke_suite` | 生成 acceptance journeys |
| `run_acceptance_suite` | 跑冒烟并写 last-run.json |
