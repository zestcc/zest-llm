# 外置适配器插件开发指南

外置插件用于 **不修改主 JAR** 扩展 ZestLLM SPI（首次放入 JAR 目录需重启加载类；SPI 切换可走 Admin 插件中心）。

## 1. 目录配置

```yaml
zest:
  llm:
    plugins:
      external-dir: /opt/zest-llm/plugins
```

或环境变量：

```bash
export ZEST_LLM_PLUGIN_DIR=/opt/zest-llm/plugins
```

## 2. 实现 SPI

在独立 Maven 模块中实现 `zest-llm-spi` 接口之一，例如：

- `cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrievalAdapter`
- `cn.zest.www.zestllm.spi.gateway.ModelGatewayAdapter`
- `cn.zest.www.zestllm.spi.observability.ObservabilityAdapter`

提供 **public 无参构造** 或 Spring `@Configuration` + `@Bean`（内置模块方式）。

外置 JAR 方式添加 SPI 描述文件，例如：

`META-INF/services/cn.zest.www.zestllm.spi.knowledge.KnowledgeRetrievalAdapter`

```
com.example.plugins.CustomKnowledgeAdapter
```

## 3. 示例骨架

```java
public class CustomKnowledgeAdapter implements KnowledgeRetrievalAdapter {
    @Override
    public String adapterId() { return "custom-kb"; }

    @Override
    public HealthStatus health() {
        return HealthStatus.builder().up(true).message("ok").build();
    }

    @Override
    public KnowledgeRetrieveResult retrieve(KnowledgeRetrieveRequest request) {
        // ...
    }
}
```

## 4. Admin 插件中心

1. **插件中心**（`/plugin-catalog`）浏览全量目录与分步集成指南
2. 设为默认 SPI → 写入 `llm_adapter_config`，并提示更新 `application.yml` 后重启
3. **集成 Setup 清单**（`/api/admin/adapters/catalog/setup-guide`）跟踪整体进度


## 内置插件模块（Phase 3）

| Maven artifact | SPI | adapterId / 配置 |
|----------------|-----|------------------|
| `zest-llm-plugin-gateway-litellm` | model-gateway | `litellm` · `zest.llm.adapters.model-gateway` |
| `zest-llm-plugin-observability-langfuse` | observability | `langfuse` · `zest.llm.adapters.observability` |
| `zest-llm-plugin-agent-runtime-native` | agent-runtime | `native` · `zest.llm.adapters.agent-runtime` |
| `zest-llm-plugin-knowledge-http` | knowledge-retrieval | `http-knowledge` · `zest.llm.adapters.knowledge-retrieval` |
| `zest-llm-plugin-knowledge-ragflow` | knowledge-retrieval | `ragflow` · `zest.llm.adapters.knowledge-retrieval` |
| `zest-llm-plugin-knowledge-dify-kb` | knowledge-retrieval | `dify-kb` · `zest.llm.adapters.knowledge-retrieval` |
| `zest-llm-plugin-agent-runtime-dify` | agent-runtime | `dify` · `zest.llm.adapters.agent-runtime` |
| `zest-llm-plugin-dify-common` | （共享配置） | `zest.llm.dify.*`（供 dify-kb / dify runtime 共用） |
| `zest-llm-plugin-knowledge-echo-sample` | knowledge-retrieval | `echo-kb`（外置 JAR 样本） |

以上模块由 `zest-llm-infra` 依赖并随 Admin 一体发布；切换 SPI 仍见下文 Admin 插件中心说明。

## 5. 与内置适配器关系

| 方式 | 何时用 |
|------|--------|
| `zest-llm-infra` 内置 | 官方维护、CI 一体发布 |
| 外置 SPI JAR | 客户私有 RAG / Gateway / Observability |
| Admin 控制台 | 目录、引导、默认 SPI 偏好 |

## 6. 限制（当前版本）

- 外置插件依赖需打包进 JAR
- 切换 Spring Bean 实现仍需 **重启 Admin**（运行时偏好已持久化，重启后对齐）
- 外置 `adapterId()` 勿与内置目录冲突

## 7. 样本插件 echo-kb

```powershell
# 编译并复制到 deploy/plugins
powershell -File deploy/scripts/build-plugin-sample.ps1

# application.yml 或环境变量
# zest.llm.plugins.external-dir=./deploy/plugins
# zest.llm.adapters.knowledge-retrieval=echo-kb

# 重启 Admin 后在插件中心查看 knowledge-retrieval:echo-kb
```
