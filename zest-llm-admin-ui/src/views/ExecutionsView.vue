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
          <p class="table-panel-subtitle">共 {{ total }} 条</p>
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

    <el-drawer v-model="drawer" title="Execution 详情" size="520px">
      <pre class="detail-json">{{ detail }}</pre>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import { adminApi, type ExecutionVO } from '../api/admin'

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
const detail = ref('')

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
    const data = await adminApi.getExecution(id)
    detail.value = JSON.stringify(data, null, 2)
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

onMounted(loadList)
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

.detail-json {
  margin: 0;
  padding: 16px;
  background: #1f2d3d;
  color: #e8eaed;
  border-radius: var(--radius-md);
  font-size: 12px;
  line-height: 1.6;
  overflow: auto;
  max-height: calc(100vh - 120px);
}
</style>
