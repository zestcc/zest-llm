<template>
  <div class="dashboard">
    <div class="runtime-strip">
      <span class="runtime-strip-label">运行时</span>
      <div class="runtime-tags">
        <span class="runtime-chip">Control Plane</span>
        <span class="runtime-chip" :class="adapterUp ? 'runtime-chip--ok' : 'runtime-chip--warn'">
          Model Gateway · {{ adapterUp ? 'UP' : 'DOWN' }}
        </span>
        <span class="runtime-chip">{{ adapterId || 'litellm' }}</span>
      </div>
    </div>

    <h3 class="section-title">治理概览</h3>
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-card-value">{{ stats.apps }}</div>
          <div class="stat-card-label">注册应用</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="stat-card">
          <div class="stat-card-value">{{ stats.tasks }}</div>
          <div class="stat-card-label">AI 作业</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="stat-card stat-card--success">
          <div class="stat-card-value">{{ stats.success }}</div>
          <div class="stat-card-label">成功调用</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="stat-card stat-card--danger">
          <div class="stat-card-value">{{ stats.failed }}</div>
          <div class="stat-card-label">失败 / 超时</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="8" :md="8">
        <div class="stat-card stat-card--wide">
          <div class="stat-card-value">{{ stats.executions }}</div>
          <div class="stat-card-label">执行总数</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8" :md="8">
        <div class="stat-card stat-card--wide">
          <div class="stat-card-value">{{ formatRate(stats.successRate) }}</div>
          <div class="stat-card-label">成功率</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8" :md="8">
        <div class="stat-card stat-card--wide">
          <div class="stat-card-value">{{ stats.todayExecutions }}</div>
          <div class="stat-card-label">今日执行</div>
        </div>
      </el-col>
    </el-row>

    <h3 class="section-title">智能体健康</h3>
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="8" :md="4">
        <div class="stat-card">
          <div class="stat-card-value">{{ agentHealth.monitored ?? 0 }}</div>
          <div class="stat-card-label">已发布 Profile</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <div class="stat-card stat-card--success">
          <div class="stat-card-value">{{ agentHealth.ready ?? 0 }}</div>
          <div class="stat-card-label">READY</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <div class="stat-card stat-card--warn">
          <div class="stat-card-value">{{ agentHealth.degraded ?? 0 }}</div>
          <div class="stat-card-label">DEGRADED</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <div class="stat-card stat-card--danger">
          <div class="stat-card-value">{{ agentHealth.unavailable ?? 0 }}</div>
          <div class="stat-card-label">UNAVAILABLE</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4">
        <div class="stat-card">
          <div class="stat-card-value">{{ agentHealth.unknown ?? 0 }}</div>
          <div class="stat-card-label">未检测</div>
        </div>
      </el-col>
    </el-row>

    <div v-if="agentAlerts.length" v-loading="agentHealthLoading" class="table-panel agent-alert-panel">
      <div class="table-panel-header">
        <div>
          <h3 class="table-panel-title">智能体告警</h3>
          <p class="table-panel-subtitle">最近一次探测非 READY 的作业</p>
        </div>
        <el-button link type="primary" @click="router.push({ path: '/ops', query: { tab: 'agent' } })">运维中心</el-button>
      </div>
      <el-table :data="agentAlerts" stripe>
        <el-table-column prop="taskCode" label="作业 Code" width="140">
          <template #default="{ row }">
            <span class="code-link" @click="goAgentConfig(row.taskCode)">{{ row.taskCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="profileVersion" label="Profile" width="100" />
        <el-table-column prop="overallStatus" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="agentStatusTag(row.overallStatus)" size="small">{{ row.overallStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="probeSource" label="来源" width="100" />
        <el-table-column prop="probedAt" label="探测时间" min-width="170" />
      </el-table>
    </div>

    <h3 class="section-title">近 7 日成本</h3>
    <div v-loading="costLoading" class="table-panel cost-panel">
      <div ref="costChartRef" class="cost-chart" />
      <el-table :data="costRows" stripe empty-text="暂无成本数据" class="cost-table">
        <el-table-column prop="date" label="日期" width="140" />
        <el-table-column prop="callCount" label="调用次数" width="120">
          <template #default="{ row }">{{ row.callCount ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="promptTokens" label="Prompt Tokens" width="140">
          <template #default="{ row }">{{ row.promptTokens ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="completionTokens" label="Completion Tokens" width="160">
          <template #default="{ row }">{{ row.completionTokens ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="totalCost" label="总成本 (USD)" min-width="120">
          <template #default="{ row }">
            {{ row.totalCost != null ? row.totalCost.toFixed(4) : '-' }}
          </template>
        </el-table-column>
      </el-table>
    </div>

    <h3 class="section-title">最近执行</h3>
    <div v-loading="loading" class="table-panel">
      <el-table :data="recentRows" stripe empty-text="暂无执行记录">
        <el-table-column prop="traceId" label="Trace ID" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="code-link" @click="goExecution(row.traceId)">{{ row.traceId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="taskCode" label="Code" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="model" label="模型" min-width="160" show-overflow-tooltip />
        <el-table-column prop="latencyMs" label="耗时" width="100">
          <template #default="{ row }">{{ row.latencyMs != null ? row.latencyMs + ' ms' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" min-width="170" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import {
  adminApi,
  normalizeCostRows,
  normalizePage,
  type AgentHealthDashboard,
  type AgentHealthItem,
  type CostDayRow,
  type ExecutionVO
} from '../api/admin'

const router = useRouter()
const loading = ref(false)
const costLoading = ref(false)
const agentHealthLoading = ref(false)
const adapterUp = ref(false)
const adapterId = ref('')
const recentRows = ref<ExecutionVO[]>([])
const costRows = ref<CostDayRow[]>([])
const costChartRef = ref<HTMLElement | null>(null)
let costChart: ECharts | null = null
const stats = reactive({
  apps: 0,
  tasks: 0,
  executions: 0,
  success: 0,
  failed: 0,
  todayExecutions: 0,
  successRate: 0,
  agentsMonitored: 0,
  agentsReady: 0,
  agentsDegraded: 0,
  agentsUnavailable: 0,
  agentsUnknown: 0
})
const agentHealth = reactive<AgentHealthDashboard>({
  monitored: 0,
  ready: 0,
  degraded: 0,
  unavailable: 0,
  unknown: 0,
  alerts: []
})
const agentAlerts = ref<AgentHealthItem[]>([])

function formatRate(rate: number) {
  if (!rate && rate !== 0) return '-'
  return `${rate.toFixed(1)}%`
}

function goExecution(traceId: string) {
  router.push({ path: '/executions', query: { traceId } })
}

function goAgentConfig(taskCode: string) {
  router.push({ path: '/agent-config', query: { task: taskCode } })
}

function agentStatusTag(status?: string) {
  if (status === 'READY') return 'success'
  if (status === 'DEGRADED') return 'warning'
  if (status === 'UNAVAILABLE') return 'danger'
  return 'info'
}

function renderCostChart() {
  if (!costChartRef.value) return
  if (!costChart) {
    costChart = echarts.init(costChartRef.value)
  }
  const dates = costRows.value.map((r) => r.date)
  const costs = costRows.value.map((r) => r.totalCost ?? 0)
  const calls = costRows.value.map((r) => r.callCount ?? 0)
  costChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['总成本 (USD)', '调用次数'], bottom: 0 },
    grid: { left: 48, right: 48, top: 24, bottom: 48 },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: dates.length > 5 ? 30 : 0 } },
    yAxis: [
      { type: 'value', name: 'USD', axisLabel: { formatter: (v: number) => v.toFixed(2) } },
      { type: 'value', name: '次数', splitLine: { show: false } }
    ],
    series: [
      {
        name: '总成本 (USD)',
        type: 'bar',
        data: costs,
        itemStyle: { color: '#667eea', borderRadius: [4, 4, 0, 0] }
      },
      {
        name: '调用次数',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: calls,
        itemStyle: { color: '#16a34a' },
        lineStyle: { width: 2 }
      }
    ]
  })
}

function handleChartResize() {
  costChart?.resize()
}

watch(costRows, async () => {
  await nextTick()
  renderCostChart()
})

async function loadFallbackStats() {
  const [appsData, tasksData, execData] = await Promise.all([
    adminApi.listApps(1, 500),
    adminApi.listTasks(1, 500),
    adminApi.listExecutions(1, 200)
  ])
  const apps = normalizePage(appsData, 1, 500).records
  const tasks = normalizePage(tasksData, 1, 500).records
  const execPage = execData
  const records = execPage?.records || []
  stats.apps = apps.length
  stats.tasks = tasks.length
  stats.executions = execPage?.total ?? records.length
  stats.success = records.filter((r) => r.status === 'SUCCESS').length
  stats.failed = records.filter((r) => r.status !== 'SUCCESS').length
  stats.successRate = stats.executions ? (stats.success / stats.executions) * 100 : 0
  recentRows.value = records.slice(0, 10)
}

onMounted(async () => {
  window.addEventListener('resize', handleChartResize)
  loading.value = true
  costLoading.value = true
  agentHealthLoading.value = true
  try {
    const [statsRes, costRes, healthRes, execRes, agentHealthRes] = await Promise.allSettled([
      adminApi.dashboardStats(),
      adminApi.dashboardCost(7),
      adminApi.adapterHealth(),
      adminApi.listExecutions(1, 10),
      adminApi.dashboardAgentHealth()
    ])

    if (statsRes.status === 'fulfilled' && statsRes.value) {
      Object.assign(stats, statsRes.value)
      agentHealth.monitored = statsRes.value.agentsMonitored ?? agentHealth.monitored
      agentHealth.ready = statsRes.value.agentsReady ?? agentHealth.ready
      agentHealth.degraded = statsRes.value.agentsDegraded ?? agentHealth.degraded
      agentHealth.unavailable = statsRes.value.agentsUnavailable ?? agentHealth.unavailable
      agentHealth.unknown = statsRes.value.agentsUnknown ?? agentHealth.unknown
    } else {
      await loadFallbackStats()
    }

    if (agentHealthRes.status === 'fulfilled' && agentHealthRes.value) {
      Object.assign(agentHealth, agentHealthRes.value)
      agentAlerts.value = agentHealthRes.value.alerts || []
    }

    if (costRes.status === 'fulfilled') {
      costRows.value = normalizeCostRows(costRes.value)
    }

    if (healthRes.status === 'fulfilled') {
      adapterUp.value = !!healthRes.value?.up
      adapterId.value = healthRes.value?.adapterId || 'litellm'
    }

    if (execRes.status === 'fulfilled') {
      const records = execRes.value?.records || []
      if (!recentRows.value.length) {
        recentRows.value = records
      }
    } else if (statsRes.status !== 'fulfilled') {
      /* fallback already loaded recent rows */
    }
  } finally {
    loading.value = false
    costLoading.value = false
    agentHealthLoading.value = false
    await nextTick()
    renderCostChart()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleChartResize)
  costChart?.dispose()
  costChart = null
})
</script>

<style scoped>
.runtime-strip {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 16px;
  padding: 12px 16px;
  margin-bottom: 8px;
  background: var(--surface-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.runtime-strip-label {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-secondary);
  white-space: nowrap;
}

.runtime-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.runtime-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--text-secondary);
  background: #f0f2f5;
}

.runtime-chip--ok {
  color: #3a7a2a;
  background: #f0f9eb;
}

.runtime-chip--warn {
  color: #b88230;
  background: #fdf6ec;
}

.stat-row {
  margin-bottom: 4px;
}

.stat-row :deep(.el-col) {
  margin-bottom: 12px;
}

.stat-card {
  background: var(--surface-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 18px 16px 16px;
  text-align: center;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  border-color: #d4dbe6;
  box-shadow: var(--shadow-md);
}

.stat-card-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  color: #2563eb;
  letter-spacing: -0.02em;
}

.stat-card--success .stat-card-value {
  color: #16a34a;
}

.stat-card--danger .stat-card-value {
  color: #dc2626;
}

.stat-card--warn .stat-card-value {
  color: #d97706;
}

.stat-card-label {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 6px;
}

.cost-panel {
  padding-top: 8px;
}

.cost-chart {
  width: 100%;
  height: 280px;
  margin-bottom: 8px;
}

.cost-table {
  margin-top: 8px;
}

.agent-alert-panel {
  margin-bottom: 20px;
}
</style>
