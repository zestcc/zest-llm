# B 整合栈 Demo 指南

> **目标**：RAGFlow 知识检索 + Dify 编排 + 蒸馏闭环，端到端可演示、可验收。  
> **环境**：Linux + Docker（Windows 本地无 Docker 时请用 Gitee `docker-e2e` 或 Linux 测试机）。

---

## 一键启动（Large + Integration）

```bash
git clone https://gitee.com/zestcc/zest-llm.git && cd zest-llm

# 可选：镜像加速
cp deploy/env.compose.example deploy/.env

bash deploy/scripts/zest-stack-up.sh large
bash deploy/scripts/wait-stack-ready.sh
bash deploy/scripts/integration-demo.sh
```

成功输出：`All B integration demo checks passed.`

---

## 场景模板

| 模板 ID | 作业 code | 模式 | 知识 provider | 编排 |
|---------|-----------|------|---------------|------|
| `report-basic` | `aiReport` | hybrid | ragflow · `report-docs` | native + LiteLLM |
| `ops-monitor` | `aiOps` | agent + tool loop | ragflow · `ops-runbook` | dify |
| `knowledge-qa` | `aiKnowledge` | agent | dify-kb · `product-faq` | native + Learning |

示例 Profile：`examples/scenarios/report/` · `ops/` · `knowledge-qa/`。

---

## 验收脚本

| 脚本 | 说明 |
|------|------|
| `deploy/scripts/integration-demo.sh` | Wizard + prepare/invoke smoke（report + ops） |
| `deploy/scripts/run-integration-acceptance.sh` | `ZEST_INTEGRATION_E2E=1` 严格 AC40–42、AC55–56 |
| `deploy/scripts/e2e-acceptance.sh` | 默认 small 栈：AC40–42 PASS；AC55–56 不可达时 **SKIP** |

### AC 对照

| ID | 场景 | 通过标准 |
|----|------|----------|
| AC40 | hybrid prepare | 响应含 `knowledgePrefetch` |
| AC41 | SPI health | `/api/admin/adapters/health/all` 含 agent-runtime、knowledge-retrieval、learning-pipeline |
| AC42 | Probe | probe 结果含 `external-runtime` 或 `knowledge` 检查项 |
| AC55 | Wizard report | `report-basic` → `aiReport` |
| AC56 | Wizard ops | `ops-monitor` → `aiOps` |

---

## 侧车健康与降级

Dify / RAGFlow 容器未就绪时：

- **Demo 脚本**：adapter 列表检查可能 FAIL，invoke 仍可能通过（noop 回退或空 chunks）。
- **默认 e2e**：integration 专属项 SKIP，不阻塞 small 栈签字。
- **严格模式**（`ZEST_INTEGRATION_E2E=1`）：侧车不可达则 FAIL。

查看适配器状态：

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8088/api/admin/adapters/health/all | jq .
```

---

## 相关文档

- 标准：`docs/AI整合与自我改进标准-完整版.md` §13–§15
- 推进状态：`docs/完整版推进状态.md`
- Gitee CI：`docs/Gitee-docker-e2e-触发指南.md`
