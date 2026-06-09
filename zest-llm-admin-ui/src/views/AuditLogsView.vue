<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row page-header-row--actions-end">
        <el-select v-model="filterAction" clearable placeholder="操作类型" style="width: 160px" @change="load">
          <el-option label="PUBLISH" value="PUBLISH" />
          <el-option label="CREATE" value="CREATE" />
          <el-option label="UPDATE" value="UPDATE" />
          <el-option label="ROTATE_TOKEN" value="ROTATE_TOKEN" />
        </el-select>
        <el-select v-model="filterResource" clearable placeholder="资源类型" style="width: 160px" @change="load">
          <el-option label="PROMPT" value="PROMPT" />
          <el-option label="APP" value="APP" />
          <el-option label="TASK" value="TASK" />
          <el-option label="QUOTA" value="QUOTA" />
          <el-option label="MODEL_ROUTE" value="MODEL_ROUTE" />
        </el-select>
        <el-button type="primary" :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <div v-loading="loading" class="table-panel">
      <div class="table-panel-header">
        <div>
          <h3 class="table-panel-title">审计日志</h3>
          <p class="table-panel-subtitle">Admin 操作留痕 · 共 {{ total }} 条</p>
        </div>
      </div>
      <el-table :data="rows" stripe empty-text="暂无审计记录">
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column prop="actor" label="操作人" width="120" />
        <el-table-column prop="action" label="操作" width="120" />
        <el-table-column prop="resourceType" label="资源类型" width="120" />
        <el-table-column prop="resourceId" label="资源 ID" min-width="140" show-overflow-tooltip />
        <el-table-column prop="detailJson" label="详情" min-width="240" show-overflow-tooltip />
      </el-table>
      <div class="pager-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { adminApi, normalizePage, type AuditLogVO } from '../api/admin'

const loading = ref(false)
const rows = ref<AuditLogVO[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filterAction = ref<string>()
const filterResource = ref<string>()

async function load() {
  loading.value = true
  try {
    const data = await adminApi.listAuditLogs(page.value, size.value, filterAction.value, filterResource.value)
    const normalized = normalizePage(data, page.value, size.value)
    rows.value = normalized.records
    total.value = normalized.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.pager-row {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 4px;
}
</style>
