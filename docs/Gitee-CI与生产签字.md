# Gitee CI 与生产签字

> 本机 Windows **无需 Docker**；生产签字在 **Linux + Docker** Runner 或测试机执行。

## 1. 流水线结构

| Job | 触发 | 内容 | 门禁 |
|-----|------|------|------|
| `build-and-test` | push/PR 自动 | `mvn test` + UI embed diff | 0 FAIL |
| `docker-e2e` | **manual**（`main`/`master`/`develop`/**`tags`**） | compose 干跑 large + small 栈 + `e2e-zeststory-zestllm.sh`（E2E-01/RAG-01）+ `production-acceptance.sh`（含 **sso-smoke** + **GATE-SSO** + **DOCKER-01**） | 五阶段 + ZestStory 全 PASS |

配置文件：

- Gitee：`.gitee/pipelines/zestllm-ci.yml`（或 `deploy/ci/gitee-pipeline.yml`）
- GitHub：`.github/workflows/zestflow-acceptance.yml`

## 2. Gitee Go 启用步骤

1. 仓库 → **流水线** → 新建 → 从仓库导入 `.gitee/pipelines/zestllm-ci.yml`
2. 确保 Runner 标签匹配（默认 shared runner 或自建 Linux runner）
3. push 到 `master` 后自动跑 `build-and-test`
4. **DOCKER-01 生产签字**：流水线页 → **手动运行** → 选择分支 `master`（或 release **tag**）→ 勾选 **`docker-e2e`** → 运行
5. 成功后下载 **Artifacts**，确认 `ci-docker-e2e.log` 含 `PASS DOCKER-01` 与 `ALL PRODUCTION ACCEPTANCE PASSED`；打 tag 发布前建议再手动跑一次留档

### 手动触发 docker-e2e（DOCKER-01）步骤

1. Gitee 仓库 → **流水线** → 进入 `zestllm-ci` 流水线
2. 点击 **手动运行** / **Run pipeline**
3. 分支选 `master`（或目标 release tag）
4. 在 manual jobs 中勾选 **`docker-e2e`**（`build-and-test` 可选，通常已由 push 验证）
5. 运行完成后 → 该次构建 → **产物/Artifacts** → 下载 `deploy/test-reports/ci-docker-e2e.log`

**通过判据**（日志末尾）：

```
PASS GATE-SSO sso-smoke   # 或 SKIP GATE-SSO（未启 SSO）
PASS DOCKER-01 Linux Docker production acceptance
=== ALL PRODUCTION ACCEPTANCE PASSED ===
```

ZestStory 跨仓用例：`e2e-zeststory-ci.log` 中 E2E-01 / RAG-01 为 PASS；E2E-02 在 Docker 栈内 SKIP（需本地 ZestStory :8080）。

### docker-e2e 归档产物

| 路径 | 说明 |
|------|------|
| `deploy/test-reports/ci-docker-e2e.log` | 完整验收 stdout（含 **DOCKER-01**） |
| `deploy/test-reports/e2e-zeststory-ci.log` | ZestStory E2E-01/RAG-01 冒烟 |
| `deploy/test-reports/validate-large-tier-compose.log` | Large Tier compose 干跑 |
| `deploy/test-reports/production-*.txt` | 分阶段明细 |
| `deploy/test-reports/signoff-*.txt` | 测试机 `run-production-signoff.sh` 附件 |
| `deploy/test-reports/mvn-test-latest.log` | 白盒 surefire |

### 自建 Runner 要求（docker-e2e）

- Linux x86_64，Docker 24+，支持 DinD 或挂载 `/var/run/docker.sock`
- 流水线已设 `DOCKER_HOST=tcp://docker:2375`、`DOCKER_TLS_CERTDIR=""`；DinD 服务需 `--tls=false`（见 `.gitee/pipelines/zestllm-ci.yml`）
- 内存建议 ≥ 8GB（compose 含 mysql/valkey/litellm/admin/demo/**kb-mock**）
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
- `deploy/test-reports/validate-large-tier-compose.log` — CI docker-e2e 干跑 large compose
- `deploy/test-reports/ci-docker-e2e.log` — CI docker-e2e 完整日志

## 4. 生产签字表（人工）

| 项 | 要求 | 证据 |
|----|------|------|
| GATE-WB | mvn test 0 FAIL | signoff 日志 Phase WHITEBOX |
| GATE-BB | e2e AC1–54 0 FAIL | production-acceptance BLACKBOX |
| GATE-SSO | sso-smoke PASS 或 SKIP（未启 SSO） | production-acceptance `GATE-SSO` 行 |
| GATE-CH | journeys PASS | production-acceptance CHAIN |
| GATE-ST | P95 ≤ 500ms，成功率 ≥ 95% | STRESS 段 |
| DOCKER-01 | Linux Docker 五阶段 + ZestStory E2E-01/RAG-01 | `PASS DOCKER-01` in `ci-docker-e2e.log` |
| GATE-SEC | P0 安全全 PASS | full-acceptance SEC-* |
| 遗留 S1/S2 | 0 未关闭 | 缺陷清单 |

签字：________ 日期：________ 版本/tag：________

## 5. Windows 本地（预检，非生产签字）

```powershell
powershell -File deploy/scripts/start-local-full.ps1 -EmbedUi -WithLiteLLM -WithDemo -SkipBuild
powershell -File deploy/scripts/production-acceptance.ps1 -Tier local
```

local Tier 阈值：P95≤800ms（压测≤1200ms），**不能替代** Docker 生产签字。

**ZestStory 跨仓 E2E**（非生产签字，联调门禁）：ZestLLM + ZestStory 均已启动后：

```powershell
powershell -File deploy/scripts/e2e-zeststory-zestllm.ps1 -SkipStart
```

期望 E2E-01 / RAG-01 / E2E-02 PASS；**DOCKER-01** 在 Gitee **手动触发 `docker-e2e`**（Linux Runner + Docker），见上文 §2「手动触发 docker-e2e」。详见 `docs/ZestStory-ZestLLM-接入报告.md` §5.2。

## 6. 故障排查

| 现象 | 处理 |
|------|------|
| ghcr.io / LiteLLM 拉取超时 | 复制 `deploy/env.compose.example` → `deploy/.env` 设 `LITELLM_IMAGE` 为国内镜像；或先 `bash deploy/scripts/preload-stack-images.sh` |
| compose 启动超时 | 增大 `wait-stack-ready.sh` 的 `MAX_RETRIES` |
| P95 超 500ms | 检查 litellm/openai-mock 健康；非 CI 环境可 `P95_MAX_MS=800` 调试 |
| *IT 失败 | CI 设 `SKIP_IT=1`；本地有 Docker 时 `SKIP_IT=0` |
| UI static diff 失败 | `bash deploy/scripts/build-admin-ui.sh` 后提交 static |
