<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row">
        <p class="page-desc">FinOps 成本告警、Execution 归档与智能体探测 Webhook 记录</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="ops-tabs">
      <el-tab-pane label="成本告警" name="cost">
        <div class="toolbar">
          <el-input v-model="costAppKey" placeholder="按 appKey 筛选" clearable style="width: 220px" @keyup.enter="loadCostAlerts" />
          <el-button type="primary" :icon="Refresh" @click="loadCostAlerts">刷新</el-button>
        </div>
        <div v-loading="costLoading" class="table-panel">
          <el-table :data="costAlerts" stripe empty-text="暂无成本告警">
            <el-table-column prop="createdAt" label="时间" width="170" />
            <el-table-column prop="appKey" label="App Key" width="140" />
            <el-table-column prop="alertDate" label="日期" width="120" />
            <el-table-column prop="dailyCost" label="当日成本" width="120">
              <template #default="{ row }">{{ formatCost(row.dailyCost) }}</template>
            </el-table-column>
            <el-table-column prop="costLimit" label="日限额" width="120">
              <template #default="{ row }">{{ formatCost(row.costLimit) }}</template>
            </el-table-column>
            <el-table-column prop="thresholdPct" label="阈值%" width="90" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'SENT' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="Execution 归档" name="archive">
        <div v-loading="archiveLoading" class="archive-panel">
          <el-row :gutter="16" class="stat-row">
            <el-col :xs="24" :sm="8">
              <div class="stat-card">
                <div class="stat-card-value">{{ archiveStats.hotExecutions ?? 0 }}</div>
                <div class="stat-card-label">热表 Execution</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="8">
              <div class="stat-card">
                <div class="stat-card-value">{{ archiveStats.archivedExecutions ?? 0 }}</div>
                <div class="stat-card-label">已归档</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="8">
              <div class="stat-card">
                <div class="stat-card-value">{{ archiveStats.retentionDays ?? '-' }}</div>
                <div class="stat-card-label">保留天数</div>
              </div>
            </el-col>
          </el-row>
          <div class="archive-actions">
            <el-tag :type="archiveStats.archiveEnabled ? 'success' : 'info'" size="large">
              自动归档 {{ archiveStats.archiveEnabled ? '已启用' : '未启用' }}
            </el-tag>
            <el-button type="primary" :loading="archiveRunning" @click="runArchive">立即归档</el-button>
            <el-button :icon="Refresh" @click="loadArchiveStats">刷新统计</el-button>
          </div>
          <p class="archive-hint">超过保留天数的 Execution 会从热表迁移至归档表，可在 Docker 配置中调整 `zest-llm.admin.execution-archive`。</p>
        </div>
      </el-tab-pane>

      <el-tab-pane label="智能体告警" name="agent">
        <div class="toolbar">
          <el-input v-model="agentTaskCode" placeholder="按 taskCode 筛选" clearable style="width: 220px" @keyup.enter="loadAgentAlerts" />
          <el-button type="primary" :icon="Refresh" @click="loadAgentAlerts">刷新</el-button>
        </div>
        <div v-loading="agentLoading" class="table-panel">
          <el-table :data="agentAlerts" stripe empty-text="暂无智能体探测告警">
            <el-table-column prop="createdAt" label="时间" width="170" />
            <el-table-column prop="taskCode" label="作业 Code" width="140">
              <template #default="{ row }">
                <span class="code-link" @click="goAgentConfig(row.taskCode)">{{ row.taskCode }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="profileVersion" label="Profile" width="100" />
            <el-table-column prop="overallStatus" label="探测状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.overallStatus)" size="small">{{ row.overallStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="Webhook" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'SENT' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="说明" min-width="240" show-overflow-tooltip />
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { adminApi, type AgentProbeAlertVO, type CostAlertVO, type ExecutionArchiveStatsVO } from '../api/admin'

const router = useRouter()
const route = useRoute()
const activeTab = ref('cost')

const validTabs = ['cost', 'archive', 'agent'] as const

function syncTabFromRoute() {
  const tab = route.query.tab
  if (typeof tab === 'string' && (validTabs as readonly string[]).includes(tab)) {
    activeTab.value = tab
  }
}

const costLoading = ref(false)
const costAppKey = ref('')
const costAlerts = ref<CostAlertVO[]>([])

const archiveLoading = ref(false)
const archiveRunning = ref(false)
const archiveStats = ref<ExecutionArchiveStatsVO>({})

const agentLoading = ref(false)
const agentTaskCode = ref('')
const agentAlerts = ref<AgentProbeAlertVO[]>([])

function formatCost(value?: number | null) {
  if (value == null) return '-'
  return Number(value).toFixed(6)
}

function statusTag(status?: string) {
  if (status === 'READY') return 'success'
  if (status === 'DEGRADED') return 'warning'
  if (status === 'UNAVAILABLE') return 'danger'
  return 'info'
}

function goAgentConfig(taskCode?: string) {
  if (!taskCode) return
  router.push({ path: '/agent-config', query: { task: taskCode } })
}

async function loadCostAlerts() {
  costLoading.value = true
  try {
    costAlerts.value = await adminApi.listCostAlerts(costAppKey.value || undefined)
  } finally {
    costLoading.value = false
  }
}

async function loadArchiveStats() {
  archiveLoading.value = true
  try {
    archiveStats.value = await adminApi.getExecutionArchiveStats()
  } finally {
    archiveLoading.value = false
  }
}

async function runArchive() {
  archiveRunning.value = true
  try {
    archiveStats.value = await adminApi.runExecutionArchive()
    ElMessage.success('归档任务已执行')
  } finally {
    archiveRunning.value = false
  }
}

async function loadAgentAlerts() {
  agentLoading.value = true
  try {
    agentAlerts.value = await adminApi.listAgentProbeAlerts(agentTaskCode.value || undefined)
  } finally {
    agentLoading.value = false
  }
}

onMounted(async () => {
  syncTabFromRoute()
  await Promise.all([loadCostAlerts(), loadArchiveStats(), loadAgentAlerts()])
})

watch(
  () => route.query.tab,
  () => syncTabFromRoute()
)
</script>

<style scoped>
.ops-tabs {
  background: var(--panel-bg, #fff);
  padding: 16px;
  border-radius: 12px;
}
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.stat-row {
  margin-bottom: 16px;
}
.stat-card {
  background: var(--surface-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 18px 16px;
  text-align: center;
}
.stat-card-value {
  font-size: 28px;
  font-weight: 700;
  color: #2563eb;
}
.stat-card-label {
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 13px;
}
.archive-panel {
  padding: 8px 0;
}
.archive-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.archive-hint {
  color: var(--text-secondary);
  font-size: 13px;
  margin: 0;
}
</style>
