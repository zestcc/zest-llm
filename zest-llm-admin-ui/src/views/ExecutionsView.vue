<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row">
        <div class="page-filters">
          <el-input
            v-model="traceId"
            placeholder="traceId 精确查询"
            class="page-filter-control page-filter-control--wide"
            clearable
            @keyup.enter="searchTrace"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="filterTaskCode"
            placeholder="作业 code"
            class="page-filter-control"
            clearable
            @keyup.enter="reloadList"
          />
          <el-select v-model="filterStatus" placeholder="状态" clearable class="page-filter-control page-filter-control--sm">
            <el-option label="SUCCESS" value="SUCCESS" />
            <el-option label="FAILED" value="FAILED" />
            <el-option label="TIMEOUT" value="TIMEOUT" />
          </el-select>
          <el-button @click="searchTrace">查询 Trace</el-button>
          <el-button type="primary" @click="reloadList">筛选</el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
        </div>
      </div>
    </div>

    <div v-loading="loading" class="table-panel">
      <div class="table-panel-header">
        <div>
          <h3 class="table-panel-title">Execution 列表</h3>
          <p class="table-panel-subtitle">共 {{ total }} 条 · 观测适配器 {{ observabilityConfig.adapterId || 'noop' }}</p>
        </div>
      </div>
      <el-table :data="rows" stripe empty-text="暂无执行记录">
        <el-table-column prop="traceId" label="Trace ID" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="code-link" @click="openDetail(row.traceId)">{{ row.traceId }}</span>
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
        <el-table-column prop="promptVersion" label="Prompt" width="90" />
        <el-table-column prop="latencyMs" label="耗时" width="100">
          <template #default="{ row }">{{ row.latencyMs != null ? row.latencyMs + ' ms' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" min-width="170" />
      </el-table>
      <div class="page-pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadList"
          @size-change="onSizeChange"
        />
      </div>
    </div>

    <el-drawer v-model="drawer" title="Execution 详情" size="560px">
      <template v-if="detail">
        <div class="detail-actions">
          <el-button
            v-if="detail.observabilityTraceUrl"
            type="primary"
            link
            tag="a"
            :href="detail.observabilityTraceUrl"
            target="_blank"
            rel="noopener"
          >
            在 Langfuse 中打开
          </el-button>
          <el-tag v-else size="small" type="info">Langfuse 未启用</el-tag>
        </div>
        <el-descriptions :column="1" border size="small" class="detail-desc">
          <el-descriptions-item label="Trace ID">{{ detail.traceId }}</el-descriptions-item>
          <el-descriptions-item label="作业">{{ detail.taskCode }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
          <el-descriptions-item label="模型">{{ detail.model || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Prompt">{{ detail.promptVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ detail.latencyMs != null ? detail.latencyMs + ' ms' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="Tokens">
            {{ detail.promptTokens ?? '-' }} / {{ detail.completionTokens ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="成本">{{ detail.cost ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ detail.createdAt || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.errorCode" label="错误">{{ detail.errorCode }} · {{ detail.errorMessage }}</el-descriptions-item>
        </el-descriptions>
        <h4 class="detail-section-title">Input</h4>
        <pre class="detail-json">{{ formatJson(detail.inputJson) }}</pre>
        <h4 class="detail-section-title">Output</h4>
        <pre class="detail-json">{{ formatJson(detail.outputJson) }}</pre>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import { adminApi, type ExecutionVO, type ObservabilityConfigVO } from '../api/admin'

const route = useRoute()
const loading = ref(false)
const rows = ref<ExecutionVO[]>([])
const traceId = ref('')
const filterTaskCode = ref('')
const filterStatus = ref('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const drawer = ref(false)
const detail = ref<ExecutionVO | null>(null)
const observabilityConfig = ref<ObservabilityConfigVO>({})

function formatJson(value?: string) {
  if (!value) return '-'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

async function loadList() {
  loading.value = true
  try {
    const pageData = await adminApi.listExecutions(
      page.value,
      pageSize.value,
      filterTaskCode.value || undefined,
      filterStatus.value || undefined
    )
    rows.value = pageData?.records || []
    total.value = pageData?.total ?? rows.value.length
  } finally {
    loading.value = false
  }
}

function reloadList() {
  page.value = 1
  loadList()
}

function onSizeChange() {
  page.value = 1
  loadList()
}

function resetFilters() {
  filterTaskCode.value = ''
  filterStatus.value = ''
  traceId.value = ''
  reloadList()
}

async function searchTrace() {
  if (!traceId.value) return
  await openDetail(traceId.value)
}

async function openDetail(id: string) {
  try {
    detail.value = await adminApi.getExecution(id)
    drawer.value = true
  } catch {
    /* handled by interceptor */
  }
}

watch(
  () => route.query.traceId,
  (id) => {
    if (typeof id === 'string' && id) {
      traceId.value = id
      openDetail(id)
    }
  },
  { immediate: true }
)

onMounted(async () => {
  observabilityConfig.value = await adminApi.getObservabilityConfig()
  await loadList()
})
</script>

<style scoped>
.page-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.page-filter-control {
  width: 160px;
}

.page-filter-control--wide {
  width: 260px;
}

.page-filter-control--sm {
  width: 120px;
}

.detail-actions {
  margin-bottom: 12px;
}

.detail-desc {
  margin-bottom: 16px;
}

.detail-section-title {
  margin: 12px 0 8px;
  font-size: 13px;
  color: var(--text-secondary, #666);
}

.detail-json {
  margin: 0;
  padding: 12px;
  background: #1f2d3d;
  color: #e8eaed;
  border-radius: var(--radius-md);
  font-size: 12px;
  line-height: 1.6;
  overflow: auto;
  max-height: 240px;
}
</style>
