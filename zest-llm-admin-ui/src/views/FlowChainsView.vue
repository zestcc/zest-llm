<template>
  <div class="page-shell">
    <div class="table-panel" v-loading="loading">
      <div class="table-panel-header">
        <h3 class="table-panel-title">ZestFlow 链注册表（DB）</h3>
        <p class="table-panel-subtitle">Flyway V12 种子 + Bootstrap 内存兜底</p>
      </div>
      <el-table :data="chains" stripe empty-text="暂无链">
        <el-table-column prop="chainCode" label="Chain Code" min-width="220" />
        <el-table-column prop="chainName" label="名称" min-width="160" />
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column prop="lifecycle" label="Lifecycle" width="120" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row.chainCode)">查看 JSON</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="detailVisible" :title="detail?.chainCode" width="760px" destroy-on-close>
      <el-input v-model="detailJson" type="textarea" :rows="18" class="prompt-editor" readonly />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { adminApi, type FlowChainVO } from '../api/admin'

const loading = ref(false)
const chains = ref<FlowChainVO[]>([])
const detailVisible = ref(false)
const detail = ref<FlowChainVO | null>(null)
const detailJson = ref('')

async function load() {
  loading.value = true
  try {
    chains.value = await adminApi.listFlowChains()
  } finally {
    loading.value = false
  }
}

async function viewDetail(chainCode: string) {
  detail.value = await adminApi.getFlowChain(chainCode)
  detailJson.value = detail.value?.chainData || ''
  detailVisible.value = true
}

load()
</script>
