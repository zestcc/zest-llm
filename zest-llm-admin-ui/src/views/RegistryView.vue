<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row page-header-row--actions-end">
        <el-button type="primary" :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <div v-loading="loading" class="table-panel">
      <div class="table-panel-header">
        <div>
          <h3 class="table-panel-title">方法注册表</h3>
          <p class="table-panel-subtitle">Starter 启动时上报的 @ZestLLM 方法元数据 · 共 {{ methods.length }} 条</p>
        </div>
      </div>
      <el-table :data="methods" stripe empty-text="暂无注册方法">
        <el-table-column prop="appKey" label="应用" width="140" />
        <el-table-column prop="code" label="Code" width="140">
          <template #default="{ row }">
            <span class="code-link">{{ row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="methodSignature" label="方法签名" min-width="240" show-overflow-tooltip />
        <el-table-column prop="outputClass" label="输出类型" min-width="180" show-overflow-tooltip />
        <el-table-column prop="registeredAt" label="注册时间" width="170" />
        <el-table-column label="输入字段" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.inputFields || '-' }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { adminApi, type RegistryMethodVO } from '../api/admin'

const loading = ref(false)
const methods = ref<RegistryMethodVO[]>([])

async function load() {
  loading.value = true
  try {
    methods.value = await adminApi.listRegistryMethods()
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
