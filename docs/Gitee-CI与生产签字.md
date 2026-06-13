# Gitee CI 与生产签字

> 本机 Windows **无需 Docker**；生产签字在 **Linux + Docker** Runner 或测试机执行。

## 1. 流水线结构

| Job | 触发 | 内容 | 门禁 |
|-----|------|------|------|
| `build-and-test` | push/PR 自动 | `mvn test` + UI embed diff | 0 FAIL |
| `docker-e2e` | **manual** | compose 全栈 + `production-acceptance.sh`（含 **sso-smoke** + **GATE-SSO**） | 五阶段全 PASS |

配置文件：

- Gitee：`.gitee/pipelines/zestllm-ci.yml`（或 `deploy/ci/gitee-pipeline.yml`）
- GitHub：`.github/workflows/zestflow-acceptance.yml`

## 2. Gitee Go 启用步骤

1. 仓库 → **流水线** → 新建 → 从仓库导入 `.gitee/pipelines/zestllm-ci.yml`
2. 确保 Runner 标签匹配（默认 shared runner 或自建 Linux runner）
3. push 到 `master` 后自动跑 `build-and-test`
4. 发布前在流水线页面 **手动触发** `docker-e2e`

### 自建 Runner 要求（docker-e2e）

- Linux x86_64，Docker 24+，支持 DinD 或挂载 `/var/run/docker.sock`
- 内存建议 ≥ 8GB（compose 含 mysql/valkey/litellm/admin/demo）
- 磁盘 ≥ 20GB

## 3. 测试机一键生产签字

```bash
git clone https://gitee.com/zestcc/zest-llm.git
cd zest-llm

# small 栈 + 四阶段验收（P95≤500ms）
bash deploy/scripts/run-production-signoff.sh

# medium（+ Langfuse / Valkey profile）
bash deploy/scripts/run-production-signoff.sh medium
```

产物：

- `deploy/test-reports/signoff-*.txt` — 签字附件
- `deploy/test-reports/production-*.txt` — 阶段明细
- `deploy/test-reports/mvn-test-latest.log`

## 4. 生产签字表（人工）

| 项 | 要求 | 证据 |
|----|------|------|
| GATE-WB | mvn test 0 FAIL | signoff 日志 Phase WHITEBOX |
| GATE-BB | e2e AC1–53 0 FAIL | production-acceptance BLACKBOX |
| GATE-SSO | sso-smoke PASS 或 SKIP（未启 SSO） | production-acceptance `GATE-SSO` 行 |
| GATE-CH | journeys PASS | production-acceptance CHAIN |
| GATE-ST | P95 ≤ 500ms，成功率 ≥ 95% | STRESS 段 |
| GATE-SEC | P0 安全全 PASS | full-acceptance SEC-* |
| 遗留 S1/S2 | 0 未关闭 | 缺陷清单 |

签字：________ 日期：________ 版本/tag：________

## 5. Windows 本地（预检，非生产签字）

```powershell
powershell -File deploy/scripts/start-local-full.ps1 -EmbedUi -WithLiteLLM -WithDemo -SkipBuild
powershell -File deploy/scripts/production-acceptance.ps1 -Tier local
```

local Tier 阈值：P95≤800ms（压测≤1200ms），**不能替代** Docker 生产签字。

## 6. 故障排查

| 现象 | 处理 |
|------|------|
| compose 启动超时 | 增大 `wait-stack-ready.sh` 的 `MAX_RETRIES` |
| P95 超 500ms | 检查 litellm/openai-mock 健康；非 CI 环境可 `P95_MAX_MS=800` 调试 |
| *IT 失败 | CI 设 `SKIP_IT=1`；本地有 Docker 时 `SKIP_IT=0` |
| UI static diff 失败 | `bash deploy/scripts/build-admin-ui.sh` 后提交 static |
