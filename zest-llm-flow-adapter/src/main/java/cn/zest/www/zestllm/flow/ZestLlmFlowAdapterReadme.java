package cn.zest.www.zestllm.flow;

/**
 * ZestLLM 流程编排适配占位说明（v0.2）。
 *
 * <p>外部业务流程编排器（如 ZestFlow）可通过 {@code ZEST_LLM} 节点类型调用
 * Control Plane 的 {@code POST /v1/llm/invoke} 接口，完成单次 AI 作业执行。</p>
 *
 * <h2>节点语义</h2>
 * <ul>
 *   <li><b>节点类型</b>：{@code ZEST_LLM}</li>
 *   <li><b>输入</b>：{@code code}（AI 作业码）、{@code inputs}（Prompt 变量）、{@code context}（可选业务上下文）</li>
 *   <li><b>输出</b>：{@code output}（结构化 AI 结果）、{@code traceId}（全链路追踪 ID）</li>
 * </ul>
 *
 * <h2>编排器职责</h2>
 * <ul>
 *   <li>维护 DAG 与节点间上下文传递</li>
 *   <li>每个 {@code ZEST_LLM} 节点仅负责一次 invoke，不做多步 AI 编排</li>
 *   <li>失败策略（重试 / 跳过 / 人工介入）由编排器配置</li>
 * </ul>
 *
 * <h2>v0.2 计划</h2>
 * <p>本模块将提供 {@code ZestLlmFlowNodeExecutor}，对接 ZestFlow {@code zestflow-starter}
 * 的节点 SPI，实现 {@code ZEST_LLM} 节点的 prepare → invoke → 上下文回填。</p>
 */
public final class ZestLlmFlowAdapterReadme {

    private ZestLlmFlowAdapterReadme() {
    }
}
