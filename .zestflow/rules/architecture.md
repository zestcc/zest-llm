# zest-llm-admin 架构与元件化规则

> **规范源（IDE 无关）**：本文件由 `zestflow-dev-init --init-dev` 生成。
> MCP 通过 `zestflow://rules/project` 加载；各 IDE 适配文件与本文件**同源**，修改后请带相同参数重新 `--force` 生成。

## 分层（必须遵守）

| 层级 | 职责 | 技术 |
|------|------|------|
| HTTP 入口 | 鉴权、参数校验、统一响应 | `@RestController`（薄，不写业务） |
| **业务逻辑（默认）** | 可编排、可观测、可热替换 | **`@ZestComponent` + `@ZestExecute`** |

**策略（full，默认）**：AI **新生成**的业务逻辑一律以 ZestFlow 元件实现；禁止新建 `*Service` 承载业务（持久化/外部调用可封装为元件内部私有类或独立适配元件）。
既有遗留 `Service` 可不主动重构，除非用户明确要求。


**前置条件**：可执行模块须引入 `zestflow-starter`，并配置 `zestflow.executor.port`；否则无 Executor、`@ZestComponent` 无法注册，MCP 工具也会失败。

## ZestFlow 元件约定

### 包名（不写死模块路径）

- 元件类包名：**`{模块根包}.component`**
- 多模块工程：由 AI 阅读工程结构后选择目标子模块；init-dev **不**指定具体模块目录。
- 落盘前用 MCP `read_project_file` / `search_sources` 确认模块根包，不要臆造路径。

### 命名与工作流

- 类名以 `Handler` 或 `Component` 结尾
- `componentId` 使用 camelCase
- `@ZestComponent("camelCaseId")` + `@ZestExecute`
- 新建/改元件前：`list_components`；改链后：`validate_chain`

### HTTP 暴露（Mode 3，本项目默认策略）

| 项 | 约定 |
|----|------|
| 入口 | 薄 `@RestController` + `ChainGateway` |
| 绑链 | 方法上 **`@ZestChain("stable.chain.key")`** 声明 chain_key；**禁止**在 Controller 写业务 |
| 请求 | `@RequestBody` 使用 `*Request` DTO；内部转 `Map` 或 Command 调 `executeByKey` |
| 响应 | 统一 `Result<*Response>`；链失败抛异常便于事务回滚 |
| 新功能 | AI **必须**生成 Controller 草稿（含 `@ZestChain`），与链/设计图一并产出 |


## 通用编码约束（AI / MCP 必须遵守）

### 持久化防腐（Repo 层）

- 元件 / Handler **禁止**直接注入 `*Mapper`。
- 中间必须有 **`*Repo` / `*Repository` 接口 + 实现**，封装 MyBatis/JPA 细节。
- 分层：`@ZestExecute` → `*Repo` → `*Mapper`；领域对象用 **DO**，对外用 **VO**。

### AI 生成唯一规则（`ai-generation-acceptance`）

- 所有 AI 生成须站在**验收标准**：对标市面成熟方案 + 检索内部 RAG，**90%** happy path 可跑。
- 高置信结果须 **record → 自动蒸馏 RAG**；禁止单节点黑盒与一次性 Prompt 敷衍。

### 新功能交付（链 + 设计图）

完整新功能须一次性交付，不得只写孤立元件：

1. `plan_chain` → 元件清单与 gap
2. `scaffold_component` → 元件 + Repo + DTO/VO（IDE Apply）
3. **`compose_chain` / 链 JSON** → `validate_chain`
4. **`gen_smoke_suite` → `run_acceptance_suite` → `validate_delivery(passed=true)`**（未完成禁止宣称交付完成）
5. **设计图 graph_data** 与链绑定（Admin 设计器可导入/同步）
6. 按 HTTP Mode 绑定入口（Mode3 含 `@ZestChain` Controller）
7. `gen_playground_scene` 验证

### 交付门禁（Delivery Gate · 强制）

- bootstrap 占位链（Seeder）**不等于** production 交付；功能须 `compose_chain` + `lifecycle=production`。
- `usable_score ≥ 0.95` 且 `blocking=0` 方可向用户声明完成（MCP `validate_delivery`）。
- 禁止单体 `@ZestExecute` >80 行承载完整用例；须按 Pattern 拆节点。

MCP 可生成链与设计草稿；**发布/写库由人在 Admin 确认**。

### 元件注册与 JavaDoc

- 每个 `@ZestExecute`（及 PARSER/PREDICATE 等）**必须有完整 JavaDoc**。
- 启动扫描时：**注解 `description` 优先**；为空则从 **JavaDoc 灌入 Admin 备注**（含 `@param`/`@return`）。
- Admin 元件列表、MCP `list_components` 均展示该备注，供 AI 建链。

### 自动生成代码的注释标准

方法 JavaDoc **至少包含**：

- 业务说明（一句话 + 边界/副作用）
- `@param` 每个参数：含义、类型语义、**必填/可选**、校验规则
- `@return` 返回值含义；无返回写 `@return void`
- 若有 `@ZestParam` / `@ZestOutput`，与 JavaDoc 保持一致

### 入参对象化（PO / DO / DTO / VO）

| 场景 | 类型命名 | 说明 |
|------|----------|------|
| 持久化实体 | `*DO` | 与表字段对应，仅在 Repo/Mapper 层 |
| 写库/内部传递 | `*PO` 或 Command | 创建/更新命令 |
| 跨层/元件入参 | `*DTO` / `*Command` / `*Query` | 超过 **2 个**平铺参数时必须用对象 |
| 校验复杂 | `*Request` | 多个校验注解或组合条件 |
| HTTP/对外响应 | `*VO` / `*Response` | Controller 返回体 |

**硬性规则**：`@ZestExecute` 方法平铺参数 **≤ 2**；否则改为 **一个** `*Command`/`*Query` DTO + `@ZestParam` 绑定。

### 配置安全（严禁破坏已有工程）

- **禁止** AI/MCP/init-dev **覆盖或删除** 已有：`application.yml`、`application-local.yml`、`application-prod.yml`、各模块 `pom.xml`（即使 `--force` 也不生效）。
- **禁止**擅自把数据源改成 **H2**；缺数据源时仅 seed `application-local.example.yml`（MySQL `root`/`root`），**不写** `application-local.yml`。
- init-dev **按缺口增量补齐**：缺 `application.yml` 则新建；缺 `zestflow.*` 则新建 `application-zestflow.yml`，并在已有 `application.yml` 末尾用 `---` **仅追加** import（不整文件替换）。
- 缺 `zestflow-starter` 依赖时生成 `.zestflow/bootstrap/zestflow-starter-dependency.snippet.xml`，由人合并进 pom。
- 已有 H2/MySQL/端口等配置**一律保留**；只补 ZestFlow 缺失项。


## 运行时参数（init-dev 注入）

| 参数 | 值 |
|------|-----|
| appCode | `zest-llm-admin` |
| Executor | `http://127.0.0.1:20550` |
| 元件化程度 | `full`（`full` 默认；可选 `hybrid`） |
| 元件包后缀 | `component` |
| HTTP 模式 | `3`（1=execute，2=chain-route，3=controller） |

## 禁止

- 禁止编造未注册 `componentId`
- 禁止 AI/MCP 自动 publish、reload、写生产配置
- 禁止在 Controller 写核心业务逻辑
- 禁止元件直接访问 Mapper（须经 Repo）
- **禁止**覆盖/删除已有 `application*.yml`、`pom.xml`；**禁止**擅自改用 H2
- MCP 不写盘；源码由 IDE Apply 落盘

## 重新生成

```bash
java -jar ~/.zestflow/tools/zestflow-dev-init.jar --init-dev --project . --force
# 可选：--componentization full|hybrid  --component-package component
#       --http-mode 1|2|3  --app-code ...  --executor-url ...
```
