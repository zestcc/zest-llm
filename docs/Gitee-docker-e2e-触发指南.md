# Gitee 手动触发 docker-e2e（无 Linux 测试机时的生产签字路径）

## 1. 启用流水线

1. Gitee 仓库 → **流水线** → 从 `.gitee/pipelines/zestllm-ci.yml` 导入（或同步 `deploy/ci/gitee-pipeline.yml`）。
2. 确认 Runner 带 **Docker 24+**（DinD 或挂载 `docker.sock`），内存建议 ≥ 8GB。
3. 流水线变量已内置 DinD：`DOCKER_HOST=tcp://docker:2375`、`DOCKER_TLS_CERTDIR=""`。

## 2. 自动 job：`build-and-test`

- **触发**：push / PR 到 `master` / `main` / `develop`。
- **内容**：`mvn test` + Admin UI embed 校验。
- **门禁**：0 FAIL。

## 3. 手动 job：`docker-e2e`（DOCKER-01 生产签字）

### 触发步骤

1. Gitee 仓库 → **流水线** → 选择 `zestllm-ci`
2. **手动运行** → 分支选 `master` 或 release **tag**
3. 勾选 manual job **`docker-e2e`** → 运行
4. 构建完成后 → **Artifacts** → 下载 `deploy/test-reports/`

### 执行顺序

1. `validate-large-tier-compose.sh`（Large compose 干跑）
2. `preload-stack-images.sh`（预拉镜像）
3. `zest-stack-up.sh small` + `wait-stack-ready.sh`（含 **kb-mock** 供 RAG-01）
4. `e2e-zeststory-zestllm.sh`（**E2E-01** / **RAG-01**；E2E-02 SKIP）
5. `production-acceptance.sh`（白盒 → ZestStory → 黑盒 → SSO → 链路 → 压测；含 **GATE-SSO**、P95≤500ms、AC1–67、**DOCKER-01**）

**通过判据**（`ci-docker-e2e.log` 末尾）：

```
PASS DOCKER-01 Linux Docker production acceptance
=== ALL PRODUCTION ACCEPTANCE PASSED ===
```

**B 整合栈（可选，需 large + integration profile）**：在 Runner 上单独执行  
`bash deploy/scripts/run-integration-acceptance.sh`（见 `docs/B整合栈Demo指南.md`）。  
当前 `docker-e2e` 仍用 **small 栈** 签字；integration 严格验收不阻塞 CI 主路径。

## 4. 下载归档

| 文件 | 说明 |
|------|------|
| `ci-docker-e2e.log` | 完整验收 stdout（**DOCKER-01**） |
| `e2e-zeststory-ci.log` | ZestStory E2E-01/RAG-01 |
| `preload-stack-images.log` | 镜像预拉 |
| `production-*.txt` | 分阶段明细 |
| `mvn-test-latest.log` | 白盒 |

## 5. ghcr 拉取失败时

在 Runner 环境或仓库 Variables 设置：

```bash
LITELLM_IMAGE=docker.m.daocloud.io/ghcr.io/berriai/litellm:main-latest
```

详见 `deploy/env.compose.example`。

## 6. 签字表

见 [Gitee-CI与生产签字.md](./Gitee-CI与生产签字.md) §4。
