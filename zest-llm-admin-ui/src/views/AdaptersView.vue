<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row page-header-row--actions-end">
        <el-button type="primary" :icon="Refresh" @click="load">刷新探测</el-button>
      </div>
    </div>

    <el-row v-loading="loading" :gutter="16" class="adapter-row">
      <el-col v-for="item in adapters" :key="item.key" :xs="24" :sm="12" :md="8" :lg="8">
        <div
          class="adapter-card"
          :class="item.up ? 'adapter-card--ok' : item.up === false ? 'adapter-card--down' : ''"
        >
          <div class="adapter-card-head">
            <div class="adapter-icon" :class="item.up ? '' : 'adapter-icon--muted'">
              <el-icon :size="22"><component :is="item.icon" /></el-icon>
            </div>
            <el-tag :type="item.up ? 'success' : item.up === false ? 'danger' : 'info'" size="small">
              {{ item.up ? 'UP' : item.up === false ? 'DOWN' : 'N/A' }}
            </el-tag>
          </div>
          <h4>{{ item.title }}</h4>
          <p class="adapter-kind">{{ item.key }}</p>
          <p class="adapter-id">{{ item.adapterId || item.configured || '-' }}</p>
          <p v-if="item.configured && item.adapterId" class="adapter-config">
            配置项 · {{ item.configured }}
          </p>
          <p class="adapter-msg">{{ item.message || '等待探测…' }}</p>
        </div>
      </el-col>
    </el-row>

    <div v-if="!loading && !adapters.length" class="table-panel">
      <el-empty description="暂无适配器健康数据" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  Connection,
  DataAnalysis,
  Document,
  Files,
  Monitor,
  Refresh,
  ScaleToOriginal,
  View
} from '@element-plus/icons-vue'
import { adminApi, type AdapterHealthVO } from '../api/admin'

interface AdapterCard extends AdapterHealthVO {
  key: string
  title: string
  icon: typeof Connection
}

const loading = ref(false)
const adapters = ref<AdapterCard[]>([])

const SPI_META: Record<string, { title: string; icon: typeof Connection }> = {
  'model-gateway': { title: 'Model Gateway', icon: Connection },
  gateway: { title: 'Model Gateway', icon: Connection },
  observability: { title: 'Observability', icon: View },
  'policy-cache': { title: 'Policy Cache', icon: Files },
  quota: { title: 'Quota', icon: ScaleToOriginal },
  audit: { title: 'Audit', icon: Document },
  'prompt-renderer': { title: 'Prompt Renderer', icon: Monitor },
  'output-schema-validator': { title: 'Schema Validator', icon: DataAnalysis },
  schema: { title: 'Schema Validator', icon: DataAnalysis },
  'agent-runtime': { title: 'Agent Runtime', icon: Connection },
  'knowledge-retrieval': { title: 'Knowledge RAG', icon: Files },
  'learning-pipeline': { title: 'Learning Pipeline', icon: DataAnalysis }
}

const SPI_ORDER = [
  'model-gateway',
  'agent-runtime',
  'knowledge-retrieval',
  'learning-pipeline',
  'observability',
  'quota',
  'policy-cache',
  'audit',
  'prompt-renderer',
  'output-schema-validator'
]

function resolveKind(row: AdapterHealthVO): string {
  return row.kind || row.adapterId || 'unknown'
}

function toCard(row: AdapterHealthVO): AdapterCard {
  const key = resolveKind(row)
  const meta = SPI_META[key] || { title: key, icon: Connection }
  return {
    ...row,
    key,
    title: meta.title,
    icon: meta.icon
  }
}

function sortAdapters(items: AdapterCard[]): AdapterCard[] {
  return [...items].sort((a, b) => {
    const ai = SPI_ORDER.indexOf(a.key)
    const bi = SPI_ORDER.indexOf(b.key)
    if (ai === -1 && bi === -1) return a.key.localeCompare(b.key)
    if (ai === -1) return 1
    if (bi === -1) return -1
    return ai - bi
  })
}

async function load() {
  loading.value = true
  try {
    const list = await adminApi.listAdapterHealth()
    adapters.value = sortAdapters((list || []).map(toCard))
  } catch {
    const gateway = await adminApi.adapterHealth().catch(() => null)
    adapters.value = gateway
      ? sortAdapters([toCard({ ...gateway, kind: 'model-gateway' })])
      : []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-header-row--actions-end {
  justify-content: flex-end;
}

.adapter-row {
  margin-bottom: 16px;
}

.adapter-row :deep(.el-col) {
  margin-bottom: 12px;
}

.adapter-card {
  background: var(--surface-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 20px;
  min-height: 180px;
  height: 100%;
}

.adapter-card--ok {
  border-color: #c6e2b5;
}

.adapter-card--down {
  border-color: #fbc4c4;
  background: linear-gradient(180deg, #fff 0%, #fef0f0 100%);
}

.adapter-card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
}

.adapter-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: #ecf5ff;
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.adapter-icon--muted {
  background: #f0f2f5;
  color: #909399;
}

.adapter-card h4 {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.adapter-kind {
  margin: 0 0 6px;
  font-family: ui-monospace, monospace;
  font-size: 11px;
  color: var(--text-muted);
  text-transform: lowercase;
}

.adapter-id {
  margin: 0 0 6px;
  font-family: ui-monospace, monospace;
  font-size: 13px;
  color: #667eea;
}

.adapter-config {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.adapter-msg {
  margin: 0;
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.5;
}
</style>
