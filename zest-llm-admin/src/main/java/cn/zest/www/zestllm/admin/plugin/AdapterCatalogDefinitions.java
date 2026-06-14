package cn.zest.www.zestllm.admin.plugin;

import java.util.List;

/**
 * ZestLLM 适配器插件全量目录（内置 + 路线图），含分步集成引导。
 */
public final class AdapterCatalogDefinitions {

    private AdapterCatalogDefinitions() {
    }

    public static List<AdapterCatalogEntry> all() {
        return List.of(
                litellmGateway(),
                langfuseObservability(),
                noopObservability(),
                nativeAgentRuntime(),
                difyAgentRuntime(),
                noopAgentRuntime(),
                httpKnowledge(),
                echoKnowledgeSample(),
                ragflowKnowledge(),
                difyKbKnowledge(),
                noopKnowledge(),
                noopLearning(),
                handlebarsPrompt(),
                caffeinePolicyCache(),
                valkeyPolicyCache(),
                noopQuota(),
                noopAudit(),
                jsonSchemaValidator(),
                keywordModeration(),
                syncReportChannel(),
                kafkaReportChannel(),
                httpAlertWebhook(),
                httpMcpTool(),
                springAiGatewayFuture(),
                oneApiGatewayFuture(),
                otelObservabilityFuture()
        );
    }

    public static String catalogKey(String spiType, String pluginId) {
        return spiType + ":" + pluginId;
    }

    public static AdapterCatalogEntry findByKey(String catalogKey) {
        if (catalogKey == null || !catalogKey.contains(":")) {
            return null;
        }
        String[] parts = catalogKey.split(":", 2);
        return findBySpiAndPlugin(parts[0], parts[1]);
    }

    public static AdapterCatalogEntry findBySpiAndPlugin(String spiType, String pluginId) {
        return all().stream()
                .filter(item -> item.getSpiType().equals(spiType) && item.getPluginId().equals(pluginId))
                .findFirst()
                .orElse(null);
    }

    private static AdapterCatalogEntry litellmGateway() {
        return entry("litellm", "LiteLLM 模型网关", "model-gateway",
                "统一 OpenAI 兼容 API，路由多 Provider、fallback 与成本统计",
                "BerriAI", "1.0", "zest.llm.adapters.model-gateway", true, true,
                List.of("LiteLLM 进程可访问", "DEEPSEEK_API_KEY 或等价上游密钥"),
                List.of("chat-basic", "generic-chat-agent"),
                """
                        zest:
                          llm:
                            adapters:
                              model-gateway: litellm
                            litellm:
                              base-url: http://127.0.0.1:4000
                        """,
                "zest-llm-plugin-gateway-litellm",
                steps(
                        step("prep-litellm", 1, "部署 LiteLLM", "启动 LiteLLM Proxy（本地可用 deploy/scripts/start-litellm-local.ps1）",
                                "COMMAND", "查看启动脚本", null,
                                "powershell -File deploy/scripts/start-litellm-local.ps1", null, true),
                        step("config-gateway", 2, "配置 Admin 网关地址", "设置 zest.llm.litellm.base-url 指向 LiteLLM",
                                "CONFIG", "zest.llm.litellm.base-url", "zest.llm.litellm.base-url", null, null, true),
                        step("select-adapter", 3, "选择适配器", "设置 zest.llm.adapters.model-gateway=litellm",
                                "CONFIG", "zest.llm.adapters.model-gateway", "zest.llm.adapters.model-gateway", null, null, true),
                        step("register-models", 4, "注册 Gateway 模型", "在集成概览 Import 模型并触发 LiteLLM 同步",
                                "NAVIGATE", "打开集成概览", "/integration", null, null, true),
                        step("verify-health", 5, "健康探测", "适配器健康页或本页「探测健康」应显示 UP",
                                "VERIFY", "探测健康", "/adapters", null, null, true)
                ));
    }

    private static AdapterCatalogEntry langfuseObservability() {
        return entry("langfuse", "Langfuse 可观测", "observability",
                "Trace / Eval / 成本归因，与自我改进闭环联动",
                "Langfuse", "1.0", "zest.llm.adapters.observability", true, true,
                List.of("Langfuse 实例或 Cloud 项目"),
                List.of("chat-basic", "ops-monitor"),
                """
                        zest:
                          llm:
                            adapters:
                              observability: langfuse
                            langfuse:
                              base-url: http://127.0.0.1:3000
                              public-key: ${LANGFUSE_PUBLIC_KEY}
                              secret-key: ${LANGFUSE_SECRET_KEY}
                        """,
                "zest-llm-plugin-observability-langfuse",
                steps(
                        step("prep-langfuse", 1, "准备 Langfuse", "部署 Langfuse 或使用 Cloud，创建 Project 并拿到 Public/Secret Key",
                                "DOC", "Langfuse 文档", null, null, "https://langfuse.com/docs", true),
                        step("config-keys", 2, "配置密钥", "设置 LANGFUSE_PUBLIC_KEY / LANGFUSE_SECRET_KEY 环境变量",
                                "CONFIG", "环境变量", "LANGFUSE_PUBLIC_KEY", null, null, true),
                        step("select-adapter", 3, "启用 Langfuse 适配器", "zest.llm.adapters.observability=langfuse",
                                "CONFIG", "zest.llm.adapters.observability", "zest.llm.adapters.observability", null, null, true),
                        step("verify-trace", 4, "验证 Trace", "Playground 发起一次调用，在 Langfuse 查看 trace",
                                "NAVIGATE", "打开 Playground", "/playground", null, null, true)
                ));
    }

    private static AdapterCatalogEntry noopObservability() {
        return entry("noop", "空可观测", "observability", "不写入外部 Trace 系统", "Zest", "1.0",
                "zest.llm.adapters.observability", true, true, List.of(), List.of(),
                "zest.llm.adapters.observability: noop", "zest-llm-infra",
                steps(step("select", 1, "选择 noop", "开发/PoC 可保持默认 noop", "CONFIG", "配置项",
                        "zest.llm.adapters.observability", null, null, true)));
    }

    private static AdapterCatalogEntry nativeAgentRuntime() {
        return entry("native", "Native Agent Runtime", "agent-runtime",
                "控制面 LiteLLM 直接生成，支持 MCP 工具循环",
                "Zest", "1.0", "zest.llm.adapters.agent-runtime", true, true,
                List.of("model-gateway 已 UP"),
                List.of("chat-basic", "generic-agent-mcp"),
                "zest.llm.adapters.agent-runtime: native", "zest-llm-plugin-agent-runtime-native",
                steps(
                        step("gateway-up", 1, "确认模型网关", "Model Gateway 健康探测通过", "VERIFY", "适配器健康", "/adapters", null, null, true),
                        step("select", 2, "选择 native", "zest.llm.adapters.agent-runtime=native", "CONFIG", "配置项",
                                "zest.llm.adapters.agent-runtime", null, null, true),
                        step("profile", 3, "配置 Agent Profile", "runtimeBackend.type=native，按需挂载 MCP",
                                "NAVIGATE", "智能体配置", "/agent-config", null, null, true)
                ));
    }

    private static AdapterCatalogEntry difyAgentRuntime() {
        return entry("dify", "Dify Agent Runtime", "agent-runtime",
                "将完整 Agent 执行委托给 Dify 应用",
                "LangGenius", "1.0", "zest.llm.adapters.agent-runtime", true, true,
                List.of("Dify 实例", "Dify App API Key"),
                List.of("generic-hybrid-rag"),
                """
                        zest:
                          llm:
                            adapters:
                              agent-runtime: dify
                            dify:
                              base-url: http://127.0.0.1/v1
                        """,
                "zest-llm-plugin-agent-runtime-dify",
                steps(
                        step("prep-dify", 1, "准备 Dify 应用", "创建 Dify 应用并获取 API Key，写入 Secret",
                                "NAVIGATE", "密钥管理", "/integration", null, null, true),
                        step("select", 2, "启用 dify runtime", "zest.llm.adapters.agent-runtime=dify",
                                "CONFIG", "配置项", "zest.llm.adapters.agent-runtime", null, null, true),
                        step("profile", 3, "Profile 指向 Dify", "runtimeBackend.type=dify，填写 appId / secretRef",
                                "NAVIGATE", "智能体配置", "/agent-config", null, null, true),
                        step("probe", 4, "探测 external-runtime", "发布前运行 Agent Probe",
                                "VERIFY", "Playground 或 Probe API", "/playground", null, null, true)
                ));
    }

    private static AdapterCatalogEntry noopAgentRuntime() {
        return entry("noop", "空 Agent Runtime", "agent-runtime", "禁用外部 Runtime 委托", "Zest", "1.0",
                "zest.llm.adapters.agent-runtime", true, true, List.of(), List.of(),
                "zest.llm.adapters.agent-runtime: noop", "zest-llm-infra",
                steps(step("select", 1, "选择 noop", "仅 Admin 配置场景使用", "CONFIG", "配置项",
                        "zest.llm.adapters.agent-runtime", null, null, true)));
    }

    private static AdapterCatalogEntry httpKnowledge() {
        return entry("http-knowledge", "HTTP 知识检索", "knowledge-retrieval",
                "对接任意 HTTP RAG API（如 ZestStory KB Mock）",
                "Zest", "1.0", "zest.llm.adapters.knowledge-retrieval", true, true,
                List.of("知识库 HTTP 检索端点"),
                List.of("knowledge-qa", "generic-hybrid-rag"),
                """
                        zest:
                          llm:
                            adapters:
                              knowledge-retrieval: http-knowledge
                            http-knowledge:
                              base-url: http://127.0.0.1:8091
                        """,
                "zest-llm-plugin-knowledge-http",
                steps(
                        step("start-kb", 1, "启动知识库服务", "本地可运行 deploy/scripts/start-kb-mock-local.ps1",
                                "COMMAND", "启动 KB Mock", null,
                                "powershell -File deploy/scripts/start-kb-mock-local.ps1", null, true),
                        step("env-override", 2, "环境变量（可选）", "ZEST_LLM_ADAPTERS_KNOWLEDGE_RETRIEVAL=http-knowledge",
                                "CONFIG", "环境变量", "ZEST_LLM_ADAPTERS_KNOWLEDGE_RETRIEVAL", null, null, false),
                        step("select", 3, "选择 http-knowledge", "zest.llm.adapters.knowledge-retrieval=http-knowledge",
                                "CONFIG", "配置项", "zest.llm.adapters.knowledge-retrieval", null, null, true),
                        step("profile-rag", 4, "Profile 启用 RAG", "knowledgeConfig.enabled=true，填写 collection",
                                "NAVIGATE", "智能体配置", "/agent-config", null, null, true),
                        step("e2e", 5, "E2E 验证", "运行 e2e-zeststory-zestllm.ps1 中 RAG-01",
                                "COMMAND", "E2E", null,
                                "powershell -File deploy/scripts/e2e-zeststory-zestllm.ps1 -SkipStart", null, false)
                ));
    }

    private static AdapterCatalogEntry echoKnowledgeSample() {
        return entry("echo-kb", "Echo 知识检索（样本）", "knowledge-retrieval",
                "外置 SPI 样本：回显 query，用于验证 plugins 目录与 ServiceLoader",
                "Zest", "1.0", "zest.llm.adapters.knowledge-retrieval", false, false,
                List.of("编译 plugins/knowledge-echo-sample 并放入 external-dir"),
                List.of("knowledge-qa"),
                """
                        zest:
                          llm:
                            plugins:
                              external-dir: ./deploy/plugins
                            adapters:
                              knowledge-retrieval: echo-kb
                        """,
                "zest-llm-plugin-knowledge-echo-sample",
                steps(
                        step("build", 1, "编译样本插件", "mvn -pl plugins/knowledge-echo-sample package",
                                "COMMAND", "构建", null,
                                "mvn -f plugins/pom.xml -pl knowledge-echo-sample package", null, true),
                        step("copy", 2, "复制 JAR", "将 target/*.jar 放入 zest.llm.plugins.external-dir",
                                "CONFIG", "external-dir", "zest.llm.plugins.external-dir", null, null, true),
                        step("restart", 3, "重启 Admin", "新 JAR 需重启以加载 ClassLoader",
                                "DOC", "外置插件指南", null, null, "docs/external-adapters.md", true),
                        step("select", 4, "选择 echo-kb", "zest.llm.adapters.knowledge-retrieval=echo-kb",
                                "CONFIG", "配置项", "zest.llm.adapters.knowledge-retrieval", null, null, true),
                        step("verify", 5, "健康探测", "插件详情页探测应 UP",
                                "VERIFY", "探测", "/plugin-catalog", null, null, true)
                ));
    }

    private static AdapterCatalogEntry ragflowKnowledge() {
        return entry("ragflow", "RAGFlow 知识库", "knowledge-retrieval",
                "对接 RAGFlow 文档检索 API", "InfiniFlow", "1.0",
                "zest.llm.adapters.knowledge-retrieval", true, true,
                List.of("RAGFlow 服务", "Dataset ID"),
                List.of("knowledge-qa"),
                """
                        zest:
                          llm:
                            adapters:
                              knowledge-retrieval: ragflow
                            ragflow:
                              base-url: http://127.0.0.1:9380
                        """,
                "zest-llm-plugin-knowledge-ragflow",
                steps(
                        step("prep", 1, "部署 RAGFlow", "安装 RAGFlow 并创建 Dataset",
                                "DOC", "RAGFlow 文档", null, null, "https://ragflow.io/docs", true),
                        step("select", 2, "选择 ragflow", "zest.llm.adapters.knowledge-retrieval=ragflow",
                                "CONFIG", "配置项", "zest.llm.adapters.knowledge-retrieval", null, null, true),
                        step("profile", 3, "Profile 配置", "knowledgeConfig.backend=ragflow，填写 datasetId",
                                "NAVIGATE", "智能体配置", "/agent-config", null, null, true)
                ));
    }

    private static AdapterCatalogEntry difyKbKnowledge() {
        return entry("dify-kb", "Dify 知识库", "knowledge-retrieval",
                "使用 Dify Dataset 检索", "LangGenius", "1.0",
                "zest.llm.adapters.knowledge-retrieval", true, true,
                List.of("Dify 知识库 Dataset"),
                List.of("generic-hybrid-rag"),
                "zest.llm.adapters.knowledge-retrieval: dify-kb", "zest-llm-plugin-knowledge-dify-kb",
                steps(
                        step("prep", 1, "创建 Dify Dataset", "在 Dify 上传文档并记录 Dataset ID",
                                "DOC", "Dify KB", null, null, "https://docs.dify.ai", true),
                        step("select", 2, "选择 dify-kb", "zest.llm.adapters.knowledge-retrieval=dify-kb",
                                "CONFIG", "配置项", "zest.llm.adapters.knowledge-retrieval", null, null, true)
                ));
    }

    private static AdapterCatalogEntry noopKnowledge() {
        return entry("noop", "空知识检索", "knowledge-retrieval", "不调用外部 RAG", "Zest", "1.0",
                "zest.llm.adapters.knowledge-retrieval", true, true, List.of(), List.of("chat-basic"),
                "zest.llm.adapters.knowledge-retrieval: noop", "zest-llm-infra",
                steps(step("select", 1, "保持 noop", "纯对话场景默认即可", "CONFIG", "配置项",
                        "zest.llm.adapters.knowledge-retrieval", null, null, true)));
    }

    private static AdapterCatalogEntry noopLearning() {
        return entry("noop", "空学习流水线", "learning-pipeline", "关闭 Eval 自动闭环", "Zest", "1.0",
                "zest.llm.adapters.learning-pipeline", true, true, List.of(), List.of(),
                "zest.llm.adapters.learning-pipeline: noop", "zest-llm-infra",
                steps(step("select", 1, "默认 noop", "需要自我改进时再启用 zest-eval", "CONFIG", "配置项",
                        "zest.llm.adapters.learning-pipeline", null, null, true)));
    }

    private static AdapterCatalogEntry handlebarsPrompt() {
        return entry("handlebars", "Handlebars 模板", "prompt-renderer", "Prompt 变量渲染", "Zest", "1.0",
                "zest.llm.adapters.prompt-renderer", true, true, List.of(), List.of("chat-basic"),
                "zest.llm.adapters.prompt-renderer: handlebars", "zest-llm-infra",
                steps(step("default", 1, "默认已启用", "一般无需修改", "CONFIG", "配置项",
                        "zest.llm.adapters.prompt-renderer", null, null, true)));
    }

    private static AdapterCatalogEntry caffeinePolicyCache() {
        return entry("caffeine", "Caffeine 策略缓存", "policy-cache", "进程内路由策略缓存", "Zest", "1.0",
                "zest.llm.adapters.policy-cache", true, true, List.of(), List.of(),
                "zest.llm.adapters.policy-cache: caffeine", "zest-llm-infra",
                steps(step("default", 1, "单机默认", "中小部署推荐 caffeine", "CONFIG", "配置项",
                        "zest.llm.adapters.policy-cache", null, null, true)));
    }

    private static AdapterCatalogEntry valkeyPolicyCache() {
        return entry("valkey", "Valkey/Redis 策略缓存", "policy-cache", "多实例共享策略缓存", "Zest", "1.0",
                "zest.llm.adapters.policy-cache", true, true, List.of("Redis/Valkey"),
                List.of(),
                "zest.llm.adapters.policy-cache: valkey", "zest-llm-infra",
                steps(
                        step("redis", 1, "准备 Redis", "配置 spring.data.redis 连接", "CONFIG", "Redis", "spring.data.redis.host", null, null, true),
                        step("select", 2, "选择 valkey", "zest.llm.adapters.policy-cache=valkey", "CONFIG", "配置项",
                                "zest.llm.adapters.policy-cache", null, null, true)
                ));
    }

    private static AdapterCatalogEntry noopQuota() {
        return entry("noop", "空配额", "quota", "不启用配额限流", "Zest", "1.0",
                "zest.llm.adapters.quota", true, true, List.of(), List.of(),
                "zest.llm.adapters.quota: noop", "zest-llm-infra",
                steps(step("select", 1, "默认 noop", "生产可在应用管理配置 quota", "NAVIGATE", "应用管理", "/apps", null, null, true)));
    }

    private static AdapterCatalogEntry noopAudit() {
        return entry("noop", "空审计", "audit", "审计仅写本地日志", "Zest", "1.0",
                "zest.llm.adapters.audit", true, true, List.of(), List.of(),
                "zest.llm.adapters.audit: noop", "zest-llm-infra",
                steps(step("select", 1, "默认 noop", "可查看 Admin 审计日志", "NAVIGATE", "审计日志", "/audit-logs", null, null, true)));
    }

    private static AdapterCatalogEntry jsonSchemaValidator() {
        return entry("json", "JSON Schema 校验", "output-schema-validator", "结构化输出校验", "Zest", "1.0",
                "zest.llm.adapters.output-schema-validator", true, true, List.of(), List.of("report-basic"),
                "zest.llm.adapters.output-schema-validator: json", "zest-llm-infra",
                steps(step("default", 1, "默认 json", "Prompt 版本可附 outputSchema", "NAVIGATE", "Prompt 管理", "/prompts", null, null, true)));
    }

    private static AdapterCatalogEntry keywordModeration() {
        return entry("keyword-blocklist", "关键词护栏", "content-moderation", "简单关键词拦截", "Zest", "1.0",
                "zest.llm.adapters.content-moderation", true, true, List.of(), List.of(),
                "zest.llm.adapters.content-moderation: keyword-blocklist", "zest-llm-infra",
                steps(step("default", 1, "默认启用", "可在配置中切换 noop", "CONFIG", "配置项",
                        "zest.llm.adapters.content-moderation", null, null, true)));
    }

    private static AdapterCatalogEntry syncReportChannel() {
        return entry("sync", "同步上报", "report-channel", "执行结果同步写入", "Zest", "1.0",
                "zest.llm.adapters.report-channel", true, true, List.of(), List.of(),
                "zest.llm.adapters.report-channel: sync", "zest-llm-infra",
                steps(step("default", 1, "默认 sync", "Starter 同步回调业务", "DOC", "集成文档", null, null,
                        "docs/ZestLLM-Integration-Suite.md", true)));
    }

    private static AdapterCatalogEntry kafkaReportChannel() {
        return entry("kafka", "Kafka 上报", "report-channel", "异步 Kafka 执行报告", "Apache", "1.0",
                "zest.llm.adapters.report-channel", true, true, List.of("Kafka 集群"),
                List.of(),
                "zest.llm.adapters.report-channel: kafka", "zest-llm-infra",
                steps(
                        step("kafka", 1, "配置 Kafka", "spring.kafka.bootstrap-servers", "CONFIG", "Kafka",
                                "spring.kafka.bootstrap-servers", null, null, true),
                        step("select", 2, "选择 kafka", "zest.llm.adapters.report-channel=kafka", "CONFIG", "配置项",
                                "zest.llm.adapters.report-channel", null, null, true)
                ));
    }

    private static AdapterCatalogEntry httpAlertWebhook() {
        return entry("http", "HTTP 告警 Webhook", "alert-webhook", "Probe 失败 HTTP 通知", "Zest", "1.0",
                "zest.llm.adapters.alert-webhook", true, true, List.of(), List.of("ops-monitor"),
                "zest.llm.adapters.alert-webhook: http", "zest-llm-infra",
                steps(step("ops", 1, "运维中心配置", "在运维中心填写 webhook URL", "NAVIGATE", "运维中心", "/ops", null, null, true)));
    }

    private static AdapterCatalogEntry httpMcpTool() {
        return entry("http-mcp", "HTTP MCP 工具桥", "mcp-tool",
                "通过 HTTP 调用 MCP Server 工具（Profile 级配置 Server）",
                "Zest", "1.0", "zest.llm.adapters.mcp-tool", true, true,
                List.of("MCP Server 可访问"),
                List.of("generic-agent-mcp", "ops-monitor"),
                "内置 HttpMcpToolAdapter，Profile 引用 llm_mcp_server",
                "zest-llm-infra",
                steps(
                        step("register-mcp", 1, "注册 MCP Server", "Admin 录入 MCP baseUrl 与密钥",
                                "NAVIGATE", "智能体配置 MCP", "/agent-config", null, null, true),
                        step("profile-tools", 2, "Profile 挂载工具", "mcpTools 列表引用 serverCode",
                                "NAVIGATE", "智能体配置", "/agent-config", null, null, true),
                        step("verify", 3, "Playground 验证", "带 tools 的请求应触发 tool call",
                                "NAVIGATE", "Playground", "/playground", null, null, true)
                ));
    }

    private static AdapterCatalogEntry springAiGatewayFuture() {
        return entry("spring-ai", "Spring AI Gateway", "model-gateway",
                "Spring AI 统一模型抽象（路线图）", "Spring", "0.1",
                "zest.llm.adapters.model-gateway", false, false,
                List.of("Spring AI 依赖"),
                List.of(),
                "# 添加 zest-llm-plugin-gateway-spring-ai 模块后启用", "zest-llm-plugin-gateway-spring-ai",
                steps(step("roadmap", 1, "路线图", "关注 Release Note 或提交 Issue 跟踪", "DOC", "文档", null, null,
                        "docs/AI整合与自我改进标准-完整版.md", false)));
    }

    private static AdapterCatalogEntry oneApiGatewayFuture() {
        return entry("oneapi", "OneAPI Gateway", "model-gateway",
                "OneAPI 聚合网关（路线图）", "Community", "0.1",
                "zest.llm.adapters.model-gateway", false, false,
                List.of(), List.of(),
                "# Phase 3 外置插件", "zest-llm-plugin-gateway-oneapi",
                steps(step("external", 1, "外置 JAR", "参考 docs/external-adapters.md 开发 SPI 插件",
                        "DOC", "外置插件指南", null, null, "docs/external-adapters.md", false)));
    }

    private static AdapterCatalogEntry otelObservabilityFuture() {
        return entry("otel", "OpenTelemetry", "observability",
                "OTLP Trace/Metrics（路线图）", "CNCF", "0.1",
                "zest.llm.adapters.observability", false, false,
                List.of("OTel Collector"), List.of(),
                "# Phase 3", "zest-llm-plugin-observability-otel",
                steps(step("roadmap", 1, "路线图", "优先使用 Langfuse，OTel 后续版本", "DOC", "SPI 文档", null, null,
                        "docs/AI整合与自我改进标准-完整版.md", false)));
    }

    private static AdapterCatalogEntry entry(String id, String name, String spiType, String desc, String vendor,
                                             String version, String configProperty, boolean builtIn, boolean installed,
                                             List<String> prerequisites, List<String> relatedTemplates,
                                             String configExample, String mavenArtifact,
                                             List<AdapterIntegrationStep> steps) {
        return AdapterCatalogEntry.builder()
                .pluginId(id)
                .pluginName(name)
                .spiType(spiType)
                .description(desc)
                .vendor(vendor)
                .version(version)
                .configProperty(configProperty)
                .configExample(configExample)
                .mavenArtifact(mavenArtifact)
                .builtIn(builtIn)
                .installed(installed)
                .prerequisites(prerequisites)
                .relatedTemplates(relatedTemplates)
                .integrationSteps(steps)
                .build();
    }

    private static List<AdapterIntegrationStep> steps(AdapterIntegrationStep... items) {
        return List.of(items);
    }

    private static AdapterIntegrationStep step(String stepId, int order, String title, String description,
                                                 String actionType, String actionLabel, String actionTarget,
                                                 String commandExample, String docUrl, boolean required) {
        return AdapterIntegrationStep.builder()
                .stepId(stepId)
                .order(order)
                .title(title)
                .description(description)
                .actionType(actionType)
                .actionLabel(actionLabel)
                .actionTarget(actionTarget)
                .commandExample(commandExample)
                .docUrl(docUrl)
                .required(required)
                .build();
    }
}
