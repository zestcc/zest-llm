<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row">
        <p class="page-desc">
          A 层能力栈：按规模选择 LiteLLM / Langfuse / Dify / RAGFlow 等组件组合（类比 XXL-Job 执行器集群选型）
        </p>
        <el-button type="primary" :icon="Refresh" @click="load">刷新</el-button>
        <el-button @click="exportEnv">导出 Compose 环境变量</el-button>
      </div>
    </div>

    <el-row v-loading="loading" :gutter="16">
      <el-col :span="24">
        <div class="table-panel tier-banner">
          <el-tag type="primary" size="large">当前 Tier：{{ stack?.currentTier || '-' }}</el-tag>
          <code class="deploy-cmd">{{ stack?.deployCommand }}</code>
        </div>
      </el-col>
    </el-row>

    <el-row v-loading="loading" :gutter="16" class="tier-row">
      <el-col v-for="tier in stack?.tiers || []" :key="tier.id" :xs="24" :md="8">
        <div class="table-panel tier-card" :class="{ 'tier-card--active': tier.id === stack?.currentTier }">
          <h3>{{ tier.name }}</h3>
          <p class="tier-desc">{{ tier.description }}</p>
          <p class="tier-qps">{{ tier.expectedQps }}</p>
          <el-tag v-for="c in tier.components || []" :key="c" size="small" class="tier-tag">{{ c }}</el-tag>
          <p class="tier-hint">{{ tier.composeHint }}</p>
        </div>
      </el-col>
    </el-row>

    <div v-loading="loading" class="table-panel">
      <div class="table-panel-header">
        <h3 class="table-panel-title">SPI 健康状态</h3>
      </div>
      <el-table :data="stack?.adapters || []" stripe empty-text="暂无数据">
        <el-table-column prop="kind" label="组件" width="180" />
        <el-table-column prop="configured" label="配置项" width="140" />
        <el-table-column prop="adapterId" label="实现" width="140" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.up ? 'success' : 'danger'" size="small">{{ row.up ? 'UP' : 'DOWN' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="消息" min-width="200" show-overflow-tooltip />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { adminApi, type CapabilityStackVO } from '../api/admin'

const loading = ref(false)
const stack = ref<CapabilityStackVO | null>(null)

async function load() {
  loading.value = true
  try {
    const res = await adminApi.getCapabilityStack()
    stack.value = res.data ?? res
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function exportEnv() {
  const tier = stack.value?.currentTier || 'small'
  const res = await adminApi.exportCapabilityStack(tier)
  const data = res.data ?? res
  const text = Object.entries(data)
    .map(([k, v]) => `${k}=${v}`)
    .join('\n')
  await navigator.clipboard.writeText(text)
  ElMessage.success('已复制 Compose 环境变量到剪贴板')
}
</script>

<style scoped>
.tier-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.deploy-cmd {
  font-size: 12px;
  background: var(--el-fill-color-light);
  padding: 6px 10px;
  border-radius: 6px;
}
.tier-row {
  margin-bottom: 16px;
}
.tier-card {
  height: 100%;
  margin-bottom: 16px;
}
.tier-card--active {
  border: 1px solid var(--el-color-primary);
}
.tier-desc {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  min-height: 40px;
}
.tier-qps {
  font-weight: 500;
  margin: 8px 0;
}
.tier-tag {
  margin: 0 4px 4px 0;
}
.tier-hint {
  margin-top: 12px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
</style>
