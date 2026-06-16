package cn.zest.www.zestllm.admin.plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 各插件差异化集成指引（与 {@link AdapterCatalogDefinitions} 步骤互补）。
 */
public final class AdapterPluginGuideRegistry {

    private static final Map<String, AdapterPluginGuide> GUIDES = buildGuides();

    private AdapterPluginGuideRegistry() {
    }

    public static AdapterPluginGuide forKey(String catalogKey) {
        return GUIDES.getOrDefault(catalogKey, fallbackGuide(catalogKey));
    }

    private static Map<String, AdapterPluginGuide> buildGuides() {
        Map<String, AdapterPluginGuide> map = new LinkedHashMap<>();
        map.put("model-gateway:litellm", litellm());
        map.put("observability:langfuse", langfuse());
        map.put("observability:noop", noopObservability());
        map.put("agent-runtime:native", nativeRuntime());
        map.put("agent-runtime:dify", difyRuntime());
        map.put("agent-runtime:noop", noopAgentRuntime());
        map.put("knowledge-retrieval:http-knowledge", httpKnowledge());
        map.put("knowledge-retrieval:echo-kb", echoKnowledge());
        map.put("knowledge-retrieval:ragflow", ragflow());
        map.put("knowledge-retrieval:dify-kb", difyKb());
        map.put("knowledge-retrieval:noop", noopKnowledge());
        map.put("learning-pipeline:noop", noopLearning());
        map.put("prompt-renderer:handlebars", handlebars());
        map.put("policy-cache:caffeine", caffeineCache());
        map.put("policy-cache:valkey", valkeyCache());
        map.put("quota:noop", noopQuota());
        map.put("audit:noop", noopAudit());
        map.put("output-schema-validator:json", jsonSchema());
        map.put("content-moderation:keyword-blocklist", keywordModeration());
        map.put("report-channel:sync", syncReport());
        map.put("report-channel:kafka", kafkaReport());
        map.put("alert-webhook:http", httpAlert());
        map.put("mcp-tool:http-mcp", httpMcp());
        map.put("model-gateway:spring-ai", springAiFuture());
        map.put("model-gateway:oneapi", oneApiFuture());
        map.put("observability:otel", otelFuture());
        return Map.copyOf(map);
    }

    private static AdapterPluginGuide litellm() {
        return AdapterPluginGuide.builder()
                .tagline("统一 OpenAI 兼容入口，多 Provider 路由与 fallback 的核心网关")
                .overview("""
                        LiteLLM 是 Zest Stack **small/medium/large** 各档的默认模型网关。业务 Agent 与 Admin Playground \
                        均通过 OpenAI 兼容 API 调用模型，Control Plane 只负责下发 routePolicy，不转发 token 流。
                        
                        启用后需在 Admin **集成概览** 注册 Gateway 模型并触发同步，否则 prepare 可能因找不到模型而失败。\
                        本地开发可用 mock 配置，无需真实 API Key。""")
                .useCases(List.of(
                        "多模型统一接入（OpenAI / DeepSeek / Claude / 通义 / Ollama）",
                        "主备 fallback 与成本统计",
                        "Demo / Playground / Eval 批量调用",
                        "Agent 模式直连推理（prepare 后业务侧 execute）"))
                .whenNotToUse(List.of(
                        "已有 Spring AI 统一抽象且不愿引入 LiteLLM 进程（见路线图 spring-ai）",
                        "仅需单一固定模型、无路由需求且可接受硬编码 endpoint"))
                .recommendedTier("all")
                .architectureFlow("""
                        @ZestLLM / Playground
                          → prepare (CP 返回 model + routePolicy)
                          → Agent → LiteLLM :4000/v1/chat/completions
                          → report (CP 异步审计)
                        """)
                .configRefs(List.of(
                        cfg("zest.llm.adapters.model-gateway", "SPI 选择，固定 litellm", true, "litellm", "ZEST_LLM_ADAPTERS_MODEL_GATEWAY"),
                        cfg("zest.llm.litellm.base-url", "LiteLLM Proxy 根地址", true, "http://127.0.0.1:4000", null),
                        cfg("zest.llm.litellm.api-key", "调用 LiteLLM 的 Bearer（可选）", false, "sk-...", "LITELLM_API_KEY")))
                .troubleshooting(List.of(
                        ts("Dashboard 显示 Gateway DOWN", "确认 LiteLLM 进程已启动；curl http://127.0.0.1:4000/health"),
                        ts("prepare 成功但 invoke 502", "检查 LiteLLM config.yaml 中模型名与 Admin Gateway 模型注册是否一致"),
                        ts("真实模型 401", "在 LiteLLM 或 compose 环境变量中配置上游 DEEPSEEK_API_KEY 等")))
                .relatedPlugins(List.of("model-gateway:spring-ai", "model-gateway:oneapi"))
                .docLinks(List.of(
                        link("LiteLLM 官方文档", "https://docs.litellm.ai"),
                        link("本地 mock 启动", "deploy/scripts/start-litellm-local.ps1"),
                        link("集成概览 Import", "/integration")))
                .build();
    }

    private static AdapterPluginGuide langfuse() {
        return AdapterPluginGuide.builder()
                .tagline("Trace / Eval / 成本归因，支撑可观测与自我改进闭环")
                .overview("""
                        Langfuse 适配器在每次 invoke 旁路上报 Trace，Execution 详情页可跳转 Langfuse UI。\
                        与 **Learning** 模块联动时可从 Langfuse 拉取失败样本生成 Eval 建议。
                        
                        medium Tier Compose 已内置 Langfuse；small 本地可 noop，生产 medium+ 强烈建议启用。""")
                .useCases(List.of(
                        "按 traceId 跨系统排障",
                        "Prompt 版本 A/B 与成本归因",
                        "Learning 多源样本（langfuse 源）",
                        "FinOps 日成本与 Dashboard 趋势"))
                .whenNotToUse(List.of(
                        "纯 POC 且不需要外部 Trace（用 observability: noop）",
                        "已有 OTel 全栈且暂不接 Langfuse（见 otel 路线图）"))
                .recommendedTier("medium")
                .architectureFlow("""
                        invoke/report → ObservabilityAdapter → Langfuse API
                        Admin Execution 详情 → 生成 Langfuse deep link
                        Learning suggest-cases → Langfuse trace 采样
                        """)
                .configRefs(List.of(
                        cfg("zest.llm.adapters.observability", "SPI 选择", true, "langfuse", null),
                        cfg("zest.llm.langfuse.base-url", "Langfuse 实例地址", true, "http://127.0.0.1:3000", "LANGFUSE_BASE_URL"),
                        cfg("zest.llm.langfuse.public-key", "Project Public Key", true, "pk-lf-...", "LANGFUSE_PUBLIC_KEY"),
                        cfg("zest.llm.langfuse.secret-key", "Project Secret Key", true, "sk-lf-...", "LANGFUSE_SECRET_KEY")))
                .troubleshooting(List.of(
                        ts("Execution 无 Langfuse 链接", "确认 observability=langfuse 且密钥正确；noop 时不会上报"),
                        ts("Trace 延迟", "report 为异步；刷新 Langfuse UI 或检查网络到 base-url")))
                .relatedPlugins(List.of("observability:noop", "observability:otel"))
                .docLinks(List.of(link("Langfuse 文档", "https://langfuse.com/docs")))
                .build();
    }

    private static AdapterPluginGuide noopObservability() {
        return AdapterPluginGuide.builder()
                .tagline("零外部依赖：Execution 仍落库，但不写入 Langfuse/OTel")
                .overview("""
                        **noop** 可观测适配器不调用任何外部 Trace 系统。调用仍会在 Admin **执行记录** 中完整保留 \
                        input/output/token，适合开发机、CI 与对合规无外部 SaaS 要求的 POC。
                        
                        切换到 langfuse 后历史 Execution 不会回溯导入，仅新调用产生 Trace。""")
                .useCases(List.of("本地 start-local-full 默认", "CI mvn test / e2e", "无外网或暂不开 Langfuse 的项目"))
                .whenNotToUse(List.of("生产 medium+ 需要跨服务排障", "需要 Learning 从 Langfuse 拉样本"))
                .recommendedTier("small")
                .architectureFlow("invoke → NoopObservabilityAdapter（空操作）→ Execution 仅写 MySQL")
                .configRefs(List.of(cfg("zest.llm.adapters.observability", "保持 noop", false, "noop", null)))
                .troubleshooting(List.of(ts("想启用 Langfuse", "插件中心选择 langfuse 并按 4 步向导配置密钥")))
                .relatedPlugins(List.of("observability:langfuse"))
                .docLinks(List.of())
                .build();
    }

    private static AdapterPluginGuide nativeRuntime() {
        return AdapterPluginGuide.builder()
                .tagline("Zest 原生 Agent：CP prepare + 业务 Agent 直连 LiteLLM，支持 MCP 工具循环")
                .overview("""
                        **native** 是推荐的生产 Runtime。Profile 中 runtimeBackend.type=native 时，业务侧 Agent \
                        在本地执行推理、Guardrails、JSON Schema 校验与 MCP Function Call 循环，Control Plane 不参与 token 转发。
                        
                        与 **dify** 相对：native 适合 Java 业务嵌入、细粒度 Profile 治理；dify 适合已有 Dify 应用资产。""")
                .useCases(List.of("order-service Demo methodA / aiChatTools", "MCP 工具链 + LiteLLM", "Profile 发布门禁 + Probe"))
                .whenNotToUse(List.of("已有成熟 Dify 应用且希望执行完全托管在 Dify", "需要 Dify 可视化 Workflow 编排"))
                .recommendedTier("all")
                .architectureFlow("""
                        runtimeMode=agent → prepare → Agent.execute(LiteLLM)
                        Profile.tools(type=mcp) → HttpMcpToolAdapter → MCP Server
                        """)
                .configRefs(List.of(
                        cfg("zest.llm.adapters.agent-runtime", "SPI 选择", true, "native", null),
                        cfg("zest.llm.runtime-mode", "Starter 侧 agent 模式", true, "agent", null),
                        cfg("zest.llm.agent.litellm-url", "业务 Agent 直连网关", true, "http://127.0.0.1:4000", null)))
                .troubleshooting(List.of(
                        ts("仍走 invoke 不经 Agent", "确认 zest.llm.runtime-mode=agent 且 zest-llm-agent 在 classpath"),
                        ts("Tool loop 不触发", "Profile 挂载 mcpTools；MCP Server 在 Admin 注册且探测 UP")))
                .relatedPlugins(List.of("agent-runtime:dify", "agent-runtime:noop", "mcp-tool:http-mcp"))
                .docLinks(List.of(link("Agent Profile 模型", "docs/Agent配置模型.md")))
                .build();
    }

    private static AdapterPluginGuide difyRuntime() {
        return AdapterPluginGuide.builder()
                .tagline("将 Agent 执行委托给 Dify 应用 API，Zest 只做治理与审计")
                .overview("""
                        **dify** Runtime 适用于 Large Tier 或已部署 Dify 的团队：Profile 指定 Dify appId 与 secretRef，\
                        invoke 时由 DifyAgentRuntimeAdapter 转发至 Dify Chat/Message API，结果回填 Execution。
                        
                        需提前在 Dify 创建应用并获取 API Key，通过 Admin 密钥管理或 SecretRef 注入。""")
                .useCases(List.of("Large Tier B 整合栈", "复用 Dify 已发布 Agent 应用", "混合架构：治理在 Zest、执行在 Dify"))
                .whenNotToUse(List.of("无 Dify 实例", "需要 MCP 工具循环在 Java 侧细粒度控制（用 native）"))
                .recommendedTier("large")
                .architectureFlow("invoke(external) → DifyAgentRuntimeAdapter → Dify /v1/chat-messages → report")
                .configRefs(List.of(
                        cfg("zest.llm.adapters.agent-runtime", "SPI 选择", true, "dify", null),
                        cfg("zest.llm.dify.base-url", "Dify API 根路径", true, "http://dify:5001/v1", "DIFY_BASE_URL"),
                        cfg("Profile.runtimeBackend.appId", "Dify 应用 ID", true, "uuid-...", null),
                        cfg("Profile.runtimeBackend.secretRef", "API Key 密钥引用", true, "vault:dify#api-key", null)))
                .troubleshooting(List.of(
                        ts("Probe external-runtime 失败", "检查 base-url 与 secretRef；Dify 应用需已发布"),
                        ts("401 from Dify", "轮换 API Key 并更新 SecretRef")))
                .relatedPlugins(List.of("agent-runtime:native", "knowledge-retrieval:dify-kb"))
                .docLinks(List.of(link("Dify API", "https://docs.dify.ai"), link("B 整合指南", "docs/B整合栈Demo指南.md")))
                .build();
    }

    private static AdapterPluginGuide noopAgentRuntime() {
        return AdapterPluginGuide.builder()
                .tagline("禁用外部 Runtime 委托；仅 Admin 内配置与 Playground 试跑")
                .overview("业务 invoke/agent 不委托 Dify 等外部 Runtime。Profile 的 external/hybrid 模式不可用，适合纯 native 或纯 CP invoke 场景。")
                .useCases(List.of("仅使用 native + LiteLLM", "不需要 Dify 侧车"))
                .whenNotToUse(List.of("Profile runtimeBackend.type=dify 或 external"))
                .recommendedTier("small")
                .architectureFlow("agent-runtime=noop → 无 AgentRuntimeAdapter 委托")
                .configRefs(List.of(cfg("zest.llm.adapters.agent-runtime", "noop", false, "noop", null)))
                .troubleshooting(List.of())
                .relatedPlugins(List.of("agent-runtime:native"))
                .docLinks(List.of())
                .build();
    }

    private static AdapterPluginGuide httpKnowledge() {
        return AdapterPluginGuide.builder()
                .tagline("通用 HTTP RAG 桥接，对接 ZestStory KB Mock 或自建检索 API")
                .overview("""
                        **http-knowledge** 通过 HTTP 调用任意知识库检索服务（默认契约兼容 ZestStory KB Mock）。\
                        Profile 启用 knowledgeConfig 后，prepare 阶段会注入检索片段到 Prompt 上下文。
                        
                        AC54 knowledge-qa 场景模板默认走此适配器 + 本地 KB Mock。""")
                .useCases(List.of("ZestStory 联调 RAG-01", "自建向量库 REST 封装", "medium 前快速验证 RAG 链路"))
                .whenNotToUse(List.of("已标准化 RAGFlow/Dify Dataset（用专用适配器）", "纯对话无 RAG"))
                .recommendedTier("small")
                .architectureFlow("prepare → KnowledgeRetrievalAdapter.retrieve → HTTP POST → 片段注入 Prompt")
                .configRefs(List.of(
                        cfg("zest.llm.adapters.knowledge-retrieval", "SPI 选择", true, "http-knowledge", "ZEST_LLM_ADAPTERS_KNOWLEDGE_RETRIEVAL"),
                        cfg("zest.llm.http-knowledge.base-url", "检索 API 根地址", true, "http://127.0.0.1:8092", null),
                        cfg("Profile.knowledgeConfig.collection", "集合/租户标识", true, "default", null)))
                .troubleshooting(List.of(
                        ts("检索为空", "确认 KB Mock 已启动；collection 与 mock 数据一致"),
                        ts("RAG-01 FAIL", "运行 e2e-zeststory-zestllm.ps1 并对照 deploy/test-reports")))
                .relatedPlugins(List.of("knowledge-retrieval:ragflow", "knowledge-retrieval:dify-kb", "knowledge-retrieval:noop"))
                .docLinks(List.of(link("KB Mock 脚本", "deploy/scripts/start-kb-mock-local.ps1")))
                .build();
    }

    private static AdapterPluginGuide echoKnowledge() {
        return AdapterPluginGuide.builder()
                .tagline("外置 SPI 样本：验证 plugins 目录与 ServiceLoader 加载")
                .overview("""
                        **echo-kb** 不连接真实知识库，仅回显 query 文本，用于验证外置 JAR 开发流程。\
                        编译后放入 external-dir 并重启 Admin，适合插件开发者而非生产 RAG。""")
                .useCases(List.of("外置适配器 POC", "CI 验证 ExternalAdapterRegistry"))
                .whenNotToUse(List.of("任何生产 RAG 场景"))
                .recommendedTier("small")
                .architectureFlow("external-dir/*.jar → ServiceLoader → EchoKnowledgeRetrievalAdapter")
                .configRefs(List.of(
                        cfg("zest.llm.plugins.external-dir", "外置 JAR 目录", true, "./deploy/plugins", "ZEST_LLM_PLUGIN_DIR"),
                        cfg("zest.llm.adapters.knowledge-retrieval", "选择 echo-kb", true, "echo-kb", null)))
                .troubleshooting(List.of(ts("重启后仍未加载", "确认 META-INF/services 文件与 public 无参构造")))
                .relatedPlugins(List.of("knowledge-retrieval:http-knowledge"))
                .docLinks(List.of(link("外置插件开发", "docs/external-adapters.md")))
                .build();
    }

    private static AdapterPluginGuide ragflow() {
        return AdapterPluginGuide.builder()
                .tagline("对接 InfiniFlow RAGFlow Dataset 检索 API")
                .overview("""
                        **ragflow** 适配器连接 RAGFlow 服务，按 Profile 中的 datasetId 检索文档块。\
                        属于 Large Tier 知识侧车，与 Dify KB 二选一或按作业分流。""")
                .useCases(List.of("Large Tier 文档 RAG", "企业私有知识库已部署 RAGFlow"))
                .whenNotToUse(List.of("无 RAGFlow 运维能力", "仅 POC（http-knowledge + mock 更快）"))
                .recommendedTier("large")
                .architectureFlow("retrieve → RAGFlow API /api/v1/retrieval → chunks → Prompt")
                .configRefs(List.of(
                        cfg("zest.llm.adapters.knowledge-retrieval", "ragflow", true, "ragflow", null),
                        cfg("zest.llm.ragflow.base-url", "RAGFlow 地址", true, "http://127.0.0.1:9380", null),
                        cfg("Profile.knowledgeConfig.datasetId", "Dataset ID", true, "...", null)))
                .troubleshooting(List.of(ts("Dataset 404", "在 RAGFlow 控制台确认 datasetId 与 API Key")))
                .relatedPlugins(List.of("knowledge-retrieval:dify-kb", "knowledge-retrieval:http-knowledge"))
                .docLinks(List.of(link("RAGFlow 文档", "https://ragflow.io/docs")))
                .build();
    }

    private static AdapterPluginGuide difyKb() {
        return AdapterPluginGuide.builder()
                .tagline("使用 Dify 知识库 Dataset 作为检索后端")
                .overview("""
                        **dify-kb** 调用 Dify Dataset 检索 API，与 **dify** Agent Runtime 共用 Dify 实例。\
                        适合 generic-hybrid-rag 场景：治理在 Zest、知识资产在 Dify。""")
                .useCases(List.of("Dify 统一知识 + Agent", "Large Tier hybrid Profile"))
                .whenNotToUse(List.of("未使用 Dify", "已用 RAGFlow 作为主知识库"))
                .recommendedTier("large")
                .architectureFlow("retrieve → Dify Dataset API → 注入 Prompt")
                .configRefs(List.of(
                        cfg("zest.llm.adapters.knowledge-retrieval", "dify-kb", true, "dify-kb", null),
                        cfg("zest.llm.dify.base-url", "与 Dify Runtime 共用", true, "http://127.0.0.1/v1", null),
                        cfg("Profile.knowledgeConfig.datasetId", "Dify Dataset ID", true, "...", null)))
                .troubleshooting(List.of(ts("检索 403", "Dataset API Key 与 secretRef 权限")))
                .relatedPlugins(List.of("knowledge-retrieval:ragflow", "agent-runtime:dify"))
                .docLinks(List.of(link("Dify 知识库", "https://docs.dify.ai")))
                .build();
    }

    private static AdapterPluginGuide noopKnowledge() {
        return AdapterPluginGuide.builder()
                .tagline("纯对话：不调用外部 RAG，Prompt 仅含用户输入")
                .overview("chat-basic 等无知识增强场景默认配置。启用 RAG 后需在 Profile 打开 knowledgeConfig 并切换 http-knowledge/ragflow/dify-kb。")
                .useCases(List.of("通用对话", "Eval 不依赖外部 KB", "降低 POC 依赖"))
                .whenNotToUse(List.of("knowledge-qa / hybrid-rag 模板", "需要引用企业文档"))
                .recommendedTier("all")
                .architectureFlow("knowledge-retrieval=noop → prepare 跳过 retrieve")
                .configRefs(List.of(cfg("zest.llm.adapters.knowledge-retrieval", "noop", false, "noop", null)))
                .troubleshooting(List.of())
                .relatedPlugins(List.of("knowledge-retrieval:http-knowledge"))
                .docLinks(List.of())
                .build();
    }

    private static AdapterPluginGuide noopLearning() {
        return AdapterPluginGuide.builder()
                .tagline("关闭外部 Learning Pipeline；Eval/Learning UI 仍可用，自动闭环需另行启用")
                .overview("默认 noop。自我改进的 suggest-cases / run-cycle 在 Admin 内实现，不依赖外部 zest-eval 流水线。")
                .useCases(List.of("手动 Eval + Learning 审核", "audit-only 模式"))
                .whenNotToUse(List.of("需要外部 ML 流水线接管自动 publish"))
                .recommendedTier("all")
                .architectureFlow("learning-pipeline=noop → LearningCycle 仅 Admin 内部")
                .configRefs(List.of(cfg("zest.llm.adapters.learning-pipeline", "noop", false, "noop", null)))
                .troubleshooting(List.of())
                .relatedPlugins(List.of())
                .docLinks(List.of(link("Learning 模块", "/learning")))
                .build();
    }

    private static AdapterPluginGuide handlebars() {
        return AdapterPluginGuide.builder()
                .tagline("Prompt 变量渲染：{{question}}、{{context}} 等 Handlebars 语法")
                .overview("内置默认，一般无需修改。Prompt 版本发布时按 inputs/context 渲染最终 prompt 再送 LiteLLM。")
                .useCases(List.of("多变量 Prompt 模板", "RAG 注入 context 块"))
                .whenNotToUse(List.of("需 Jinja2 等其他模板引擎（需自研 SPI 插件）"))
                .recommendedTier("all")
                .architectureFlow("prepare → HandlebarsPromptRenderer.render(template, inputs)")
                .configRefs(List.of(cfg("zest.llm.adapters.prompt-renderer", "handlebars", false, "handlebars", null)))
                .troubleshooting(List.of(ts("变量未替换", "检查 Prompt 模板变量名与 @AiInput 字段一致")))
                .relatedPlugins(List.of())
                .docLinks(List.of())
                .build();
    }

    private static AdapterPluginGuide caffeineCache() {
        return AdapterPluginGuide.builder()
                .tagline("进程内 Policy 缓存：单机部署低延迟，默认推荐")
                .overview("""
                        **caffeine** 缓存 prepare 策略（Prompt 版本、路由、Profile）。Prompt 发布时 Admin 会 broadcast invalidate。\
                        多实例 Admin 需改用 valkey 共享缓存。""")
                .useCases(List.of("单机 Admin", "本地开发", "small Tier"))
                .whenNotToUse(List.of("Admin 多副本水平扩展"))
                .recommendedTier("small")
                .architectureFlow("prepare → CaffeinePolicyCache → miss 时查 MySQL")
                .configRefs(List.of(cfg("zest.llm.adapters.policy-cache", "caffeine", false, "caffeine", null)))
                .troubleshooting(List.of(ts("Prompt 更新未生效", "确认发布成功；检查 Agent policyCache TTL")))
                .relatedPlugins(List.of("policy-cache:valkey"))
                .docLinks(List.of())
                .build();
    }

    private static AdapterPluginGuide valkeyCache() {
        return AdapterPluginGuide.builder()
                .tagline("Redis/Valkey 共享 Policy 缓存，多 Admin 副本一致")
                .overview("medium+ Compose 含 Valkey。多实例 Control Plane 时必须使用，否则各副本策略缓存不一致。")
                .useCases(List.of("K8s 多副本 Admin", "medium/large Tier"))
                .whenNotToUse(List.of("单进程本地开发"))
                .recommendedTier("medium")
                .architectureFlow("prepare → ValkeyPolicyCacheAdapter → Redis/Valkey")
                .configRefs(List.of(
                        cfg("zest.llm.adapters.policy-cache", "valkey", true, "valkey", null),
                        cfg("spring.data.redis.host", "Redis 主机", true, "127.0.0.1", "REDIS_HOST"),
                        cfg("spring.data.redis.port", "端口", true, "6379", "REDIS_PORT")))
                .troubleshooting(List.of(ts("Redis 连接拒绝", "compose 中 valkey 服务是否 healthy")))
                .relatedPlugins(List.of("policy-cache:caffeine"))
                .docLinks(List.of())
                .build();
    }

    private static AdapterPluginGuide noopQuota() {
        return AdapterPluginGuide.builder()
                .tagline("SPI 层不启用配额；App 级配额可在应用管理单独配置")
                .overview("noop 表示不使用 Redis Token Bucket 等 SPI 配额实现。日 Token / QPS 仍可在 **应用管理** 写库策略。")
                .useCases(List.of("POC 无限流", "配额仅 DB 层"))
                .whenNotToUse(List.of("高并发需分布式令牌桶"))
                .recommendedTier("small")
                .architectureFlow("quota=noop → QuotaAdapter 空实现；App quota 表仍生效")
                .configRefs(List.of(cfg("zest.llm.adapters.quota", "noop", false, "noop", null)))
                .troubleshooting(List.of())
                .relatedPlugins(List.of())
                .docLinks(List.of(link("应用配额", "/apps")))
                .build();
    }

    private static AdapterPluginGuide noopAudit() {
        return AdapterPluginGuide.builder()
                .tagline("审计事件写 Admin 内置表，不外送 SIEM")
                .overview("Admin 操作审计默认走 JDBC。**audit: noop** 指 SPI AuditAdapter 不额外转发；Admin 审计日志页仍可用。")
                .useCases(List.of("默认部署"))
                .whenNotToUse(List.of("需对接外部 SIEM SPI（自研插件）"))
                .recommendedTier("all")
                .architectureFlow("Admin 操作 → llm_admin_audit_log")
                .configRefs(List.of(cfg("zest.llm.adapters.audit", "noop", false, "noop", null)))
                .troubleshooting(List.of())
                .relatedPlugins(List.of())
                .docLinks(List.of(link("审计日志", "/audit-logs")))
                .build();
    }

    private static AdapterPluginGuide jsonSchema() {
        return AdapterPluginGuide.builder()
                .tagline("结构化输出 JSON Schema 校验，report-basic 等场景门禁")
                .overview("Prompt 版本可附 outputSchema；Agent execute 后校验模型输出，失败返回 SCHEMA_VALIDATION_FAILED。")
                .useCases(List.of("报表 JSON 输出", "Eval 断言结构化字段"))
                .whenNotToUse(List.of("纯自然语言回答、无 schema"))
                .recommendedTier("all")
                .architectureFlow("execute → JsonOutputSchemaValidator.validate(output, schema)")
                .configRefs(List.of(cfg("zest.llm.adapters.output-schema-validator", "json", false, "json", null)))
                .troubleshooting(List.of(ts("校验频繁失败", "放宽 schema 或优化 Prompt 中的格式说明")))
                .relatedPlugins(List.of())
                .docLinks(List.of(link("Prompt 管理", "/prompts")))
                .build();
    }

    private static AdapterPluginGuide keywordModeration() {
        return AdapterPluginGuide.builder()
                .tagline("关键词 blocklist 内容护栏，invoke 前/后快速拦截")
                .overview("内置关键词列表可配置。适合合规初筛；复杂 moderation 需换外部 API 插件。")
                .useCases(List.of("敏感词拦截", "Demo 合规演示"))
                .whenNotToUse(List.of("需 LLM-based moderation"))
                .recommendedTier("all")
                .architectureFlow("invoke → KeywordBlocklistModerationAdapter.check(content)")
                .configRefs(List.of(cfg("zest.llm.adapters.content-moderation", "keyword-blocklist", false, "keyword-blocklist", null)))
                .troubleshooting(List.of(ts("误杀", "调整 blocklist 配置或切换 noop")))
                .relatedPlugins(List.of())
                .docLinks(List.of())
                .build();
    }

    private static AdapterPluginGuide syncReport() {
        return AdapterPluginGuide.builder()
                .tagline("执行结果同步写入 Execution，Starter 默认上报路径")
                .overview("Agent report 同步 POST /v1/llm/report，失败时本地重试队列。Kafka 适合超大流量异步解耦。")
                .useCases(List.of("Demo / 中小流量", "需要 traceId 立即可查"))
                .whenNotToUse(List.of("report 峰值压垮 CP（改 kafka）"))
                .recommendedTier("all")
                .architectureFlow("Agent.report → POST /v1/llm/report → llm_execution")
                .configRefs(List.of(cfg("zest.llm.adapters.report-channel", "sync", false, "sync", null)))
                .troubleshooting(List.of(ts("report 丢失", "检查 Agent auth-token；查看 AgentReportRetryQueue 日志")))
                .relatedPlugins(List.of("report-channel:kafka"))
                .docLinks(List.of(link("集成套件", "docs/ZestLLM-Integration-Suite.md")))
                .build();
    }

    private static AdapterPluginGuide kafkaReport() {
        return AdapterPluginGuide.builder()
                .tagline("Execution 报告异步写入 Kafka，削峰解耦 Control Plane")
                .overview("large Tier 可选。需配置 spring.kafka 与 topic；Consumer 侧需自行对接或后续版本内置。")
                .useCases(List.of("高 QPS report", "Large Tier 事件驱动架构"))
                .whenNotToUse(List.of("无 Kafka 运维", "small POC"))
                .recommendedTier("large")
                .architectureFlow("report → ReportKafkaAdapter → topic zest.llm.execution.report")
                .configRefs(List.of(
                        cfg("zest.llm.adapters.report-channel", "kafka", true, "kafka", null),
                        cfg("spring.kafka.bootstrap-servers", "Broker 列表", true, "kafka:9092", "KAFKA_BOOTSTRAP_SERVERS")))
                .troubleshooting(List.of(ts("消息积压", "扩容 Consumer 或检查 CP 消费逻辑")))
                .relatedPlugins(List.of("report-channel:sync"))
                .docLinks(List.of())
                .build();
    }

    private static AdapterPluginGuide httpAlert() {
        return AdapterPluginGuide.builder()
                .tagline("Agent Probe 失败 / 成本告警 HTTP Webhook 通知")
                .overview("与 **运维中心** 配置联动。Probe 巡检失败或 FinOps 阈值触发时 POST 到企业微信/钉钉等 Webhook。")
                .useCases(List.of("ops-monitor 场景", "Probe 告警 resend"))
                .whenNotToUse(List.of("无告警接收端"))
                .recommendedTier("medium")
                .architectureFlow("Probe FAIL → AgentProbeAlertService → HttpAlertWebhookAdapter")
                .configRefs(List.of(
                        cfg("zest.llm.adapters.alert-webhook", "http", false, "http", null),
                        cfg("App.quota.alertWebhookUrl", "按 App 配置 URL", false, "https://...", null)))
                .troubleshooting(List.of(ts("未收到告警", "运维中心 URL 与 alertThresholdPct 是否配置")))
                .relatedPlugins(List.of())
                .docLinks(List.of(link("运维中心", "/ops"), link("探测手册", "docs/智能体探测与运维手册.md")))
                .build();
    }

    private static AdapterPluginGuide httpMcp() {
        return AdapterPluginGuide.builder()
                .tagline("HTTP JSON-RPC 桥接 MCP Server，Profile 级挂载工具")
                .overview("""
                        **http-mcp** 是 MCP 工具的默认实现。Admin 注册 MCP Server（baseUrl + 鉴权），\
                        Profile.mcpTools 引用 serverCode 后，native Agent 在 LiteLLM function call 循环中调用工具。
                        
                        Demo aiChatTools 与 AC 验收依赖 MCP mock :9090。""")
                .useCases(List.of("generic-agent-mcp 模板", "订单查询等业务 Tool", "ops-monitor 运维工具"))
                .whenNotToUse(List.of("无 MCP Server", "纯对话无 tools"))
                .recommendedTier("all")
                .architectureFlow("LiteLLM tool_calls → FunctionCallLoop → HttpMcpToolAdapter → MCP Server")
                .configRefs(List.of(
                        cfg("zest.llm.adapters.mcp-tool", "http-mcp", false, "http-mcp", null),
                        cfg("Admin MCP Server", "serverCode + baseUrl", true, "order-mcp", null),
                        cfg("Profile.mcpTools", "工具列表", true, "[{serverCode, toolName}]", null)))
                .troubleshooting(List.of(
                        ts("tools 未出现在请求", "Profile 是否 publish；mcpTools 非空"),
                        ts("MCP timeout", "检查 MCP Server 健康与网络")))
                .relatedPlugins(List.of("agent-runtime:native"))
                .docLinks(List.of(link("MCP Server 管理", "/agent-config")))
                .build();
    }

    private static AdapterPluginGuide springAiFuture() {
        return AdapterPluginGuide.builder()
                .tagline("【路线图】Spring AI 统一模型抽象，替代 LiteLLM 进程")
                .overview("尚未发布。关注 Release Note；当前生产请使用 litellm。")
                .useCases(List.of("Spring 生态深度集成（未来）"))
                .whenNotToUse(List.of("当前任何生产部署"))
                .recommendedTier("small")
                .architectureFlow("未实现")
                .configRefs(List.of())
                .troubleshooting(List.of())
                .relatedPlugins(List.of("model-gateway:litellm"))
                .docLinks(List.of(link("路线图", "docs/AI整合与自我改进标准-完整版.md")))
                .build();
    }

    private static AdapterPluginGuide oneApiFuture() {
        return AdapterPluginGuide.builder()
                .tagline("【路线图】OneAPI 聚合网关外置 SPI")
                .overview("可通过 docs/external-adapters.md 自研 ModelGatewayAdapter 对接 OneAPI。")
                .useCases(List.of("已有 OneAPI 资产（需自研插件）"))
                .whenNotToUse(List.of("无 OneAPI"))
                .recommendedTier("medium")
                .architectureFlow("外置 JAR → ModelGatewayAdapter SPI")
                .configRefs(List.of())
                .troubleshooting(List.of())
                .relatedPlugins(List.of("model-gateway:litellm"))
                .docLinks(List.of(link("外置插件", "docs/external-adapters.md")))
                .build();
    }

    private static AdapterPluginGuide otelFuture() {
        return AdapterPluginGuide.builder()
                .tagline("【路线图】OpenTelemetry OTLP 导出")
                .overview("优先使用 Langfuse；OTel 适配器后续版本提供。")
                .useCases(List.of("已有 OTel Collector 全栈（未来）"))
                .whenNotToUse(List.of("当前生产（用 langfuse）"))
                .recommendedTier("medium")
                .architectureFlow("未实现")
                .configRefs(List.of())
                .troubleshooting(List.of())
                .relatedPlugins(List.of("observability:langfuse"))
                .docLinks(List.of())
                .build();
    }

    private static AdapterPluginGuide fallbackGuide(String catalogKey) {
        return AdapterPluginGuide.builder()
                .tagline("适配器插件")
                .overview("详见分步集成指南与配置示例。")
                .useCases(List.of())
                .whenNotToUse(List.of())
                .recommendedTier("all")
                .architectureFlow("")
                .configRefs(List.of())
                .troubleshooting(List.of())
                .relatedPlugins(List.of())
                .docLinks(List.of())
                .build();
    }

    private static AdapterConfigRef cfg(String key, String desc, boolean required, String example, String envVar) {
        return AdapterConfigRef.builder()
                .key(key)
                .description(desc)
                .required(required)
                .example(example)
                .envVar(envVar)
                .build();
    }

    private static AdapterTroubleshootingItem ts(String problem, String solution) {
        return AdapterTroubleshootingItem.builder().problem(problem).solution(solution).build();
    }

    private static AdapterDocLink link(String label, String url) {
        return AdapterDocLink.builder().label(label).url(url).build();
    }
}
