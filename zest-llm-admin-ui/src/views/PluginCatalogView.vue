<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row">
        <p class="page-desc">
          适配器插件中心：浏览 SPI 目录、分步集成引导与默认实现选择（参考 Grafana 数据源 / zest-monitor 插件中心）
        </p>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button :loading="rescanLoading" @click="rescanExternal">重扫外置 JAR</el-button>
      </div>
    </div>

    <el-alert
      v-if="catalog?.externalDir"
      type="info"
      :closable="false"
      show-icon
      class="external-dir-alert"
      :title="`外置目录: ${catalog.externalDir} · 已加载 ${catalog.externalPlugins?.length ?? 0} 个外置插件（新 JAR 需重启）`"
    />

    <div v-loading="guideLoading" class="table-panel setup-panel">
      <div class="table-panel-header">
        <div>
          <h3 class="table-panel-title">集成 Setup 清单</h3>
          <p class="table-panel-subtitle">
            进度 {{ setupGuide?.completedSteps ?? 0 }}/{{ setupGuide?.totalSteps ?? 0 }}
            · {{ setupGuide?.progressPercent ?? 0 }}%
          </p>
        </div>
        <el-tag :type="setupGuide?.readyForProduction ? 'success' : 'warning'" size="large">
          {{ setupGuide?.readyForProduction ? '可投产' : '待完成' }}
        </el-tag>
      </div>
      <el-progress :percentage="setupGuide?.progressPercent ?? 0" :stroke-width="10" />
      <div class="setup-steps">
        <div v-for="step in setupGuide?.steps || []" :key="step.stepId" class="setup-step">
          <el-tag :type="statusTag(step.status)" size="small">{{ step.status }}</el-tag>
          <div class="setup-step-body">
            <strong>{{ step.title }}</strong>
            <p>{{ step.description }}</p>
            <p v-if="step.hint" class="setup-hint">{{ step.hint }}</p>
          </div>
          <el-button v-if="step.navigateTo" link type="primary" @click="go(step.navigateTo)">前往</el-button>
        </div>
      </div>
    </div>

    <div class="toolbar">
      <el-select v-model="spiFilter" clearable placeholder="SPI 类型" style="width: 220px" @change="load">
        <el-option v-for="t in spiTypes" :key="t" :label="t" :value="t" />
      </el-select>
    </div>

    <el-row v-loading="loading" :gutter="16">
      <el-col v-for="item in plugins" :key="item.catalogKey" :xs="24" :sm="12" :lg="8">
        <div
          class="plugin-card"
          :class="{
            'plugin-card--active': item.active,
            'plugin-card--missing': item.loadStatus === 'NOT_INSTALLED'
          }"
          @click="openDetail(item.catalogKey)"
        >
          <div class="plugin-card-head">
            <div>
              <h4>{{ item.pluginName }}</h4>
              <code class="plugin-id">{{ item.spiType }} · {{ item.pluginId }}</code>
            </div>
            <el-tag v-if="item.active" type="success" size="small">ACTIVE</el-tag>
            <el-tag v-else-if="item.loadStatus === 'NOT_INSTALLED'" type="info" size="small">未安装</el-tag>
            <el-tag v-else type="warning" size="small">未激活</el-tag>
          </div>
          <p v-if="item.tagline" class="plugin-tagline">{{ item.tagline }}</p>
          <p v-else class="plugin-desc">{{ item.description }}</p>
          <div class="plugin-meta">
            <el-tag size="small">{{ item.vendor }}</el-tag>
            <el-tag size="small" type="info">v{{ item.version }}</el-tag>
            <el-tag v-if="item.recommendedTier" size="small" type="info">{{ tierLabel(item.recommendedTier) }}</el-tag>
            <el-tag v-if="item.external" size="small" type="warning">外置</el-tag>
            <el-tag :type="item.healthUp ? 'success' : 'danger'" size="small">
              {{ item.healthUp ? '健康' : '异常' }}
            </el-tag>
          </div>
          <el-button type="primary" link @click.stop="openDetail(item.catalogKey)">查看集成指南 →</el-button>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  adminApi,
  type AdapterCatalogItemVO,
  type AdapterCatalogPageVO,
  type IntegrationSetupChecklistVO
} from '../api/admin'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const rescanLoading = ref(false)
const guideLoading = ref(false)
const spiFilter = ref('')
const catalog = ref<AdapterCatalogPageVO | null>(null)
const setupGuide = ref<IntegrationSetupChecklistVO | null>(null)

const plugins = computed(() => catalog.value?.plugins || [])

const spiTypes = [
  'model-gateway',
  'observability',
  'agent-runtime',
  'knowledge-retrieval',
  'learning-pipeline',
  'policy-cache',
  'mcp-tool',
  'report-channel'
]

function statusTag(status: string) {
  if (status === 'done') return 'success'
  if (status === 'failed') return 'danger'
  if (status === 'warning') return 'warning'
  return 'info'
}

function tierLabel(tier?: string) {
  if (!tier) return ''
  const map: Record<string, string> = {
    small: 'Small',
    medium: 'Medium',
    large: 'Large',
    all: '全 Tier'
  }
  return map[tier] || tier
}

function go(path: string) {
  router.push(path)
}

function openDetail(catalogKey: string) {
  router.push(`/plugin-catalog/${encodeURIComponent(catalogKey)}`)
}

async function loadGuide() {
  guideLoading.value = true
  try {
    setupGuide.value = await adminApi.getIntegrationSetupGuide()
  } finally {
    guideLoading.value = false
  }
}

async function load() {
  loading.value = true
  try {
    catalog.value = await adminApi.getAdapterCatalog(spiFilter.value || undefined)
  } finally {
    loading.value = false
  }
}

async function rescanExternal() {
  rescanLoading.value = true
  try {
    const result = await adminApi.rescanExternalAdapters()
    ElMessage.success(result.message || `已加载 ${result.loaded} 个外置插件`)
    await load()
  } finally {
    rescanLoading.value = false
  }
}

onMounted(() => {
  const q = route.query.spiType
  if (typeof q === 'string' && q) {
    spiFilter.value = q
  }
  loadGuide()
  load()
})
</script>

<style scoped>
.external-dir-alert {
  margin-bottom: 16px;
}
.setup-panel {
  margin-bottom: 16px;
}
.setup-steps {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.setup-step {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--border-color);
}
.setup-step:last-child {
  border-bottom: none;
}
.setup-step-body {
  flex: 1;
}
.setup-step-body p {
  margin: 4px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}
.setup-hint {
  color: var(--text-muted) !important;
  font-size: 12px !important;
}
.toolbar {
  margin-bottom: 16px;
}
.plugin-card {
  background: var(--surface-bg);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 18px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
  min-height: 200px;
}
.plugin-card:hover {
  box-shadow: var(--shadow-md);
  border-color: #409eff;
}
.plugin-card--active {
  border-color: #95d475;
  background: linear-gradient(180deg, #fff 0%, #f6ffed 100%);
}
.plugin-card--missing {
  opacity: 0.85;
}
.plugin-card-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}
.plugin-card h4 {
  margin: 0 0 4px;
  font-size: 16px;
}
.plugin-id {
  font-size: 11px;
  color: var(--text-muted);
}
.plugin-tagline {
  font-size: 13px;
  color: var(--text-primary);
  min-height: 40px;
  margin: 0 0 8px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.plugin-desc {
  font-size: 13px;
  color: var(--text-secondary);
  min-height: 40px;
  margin: 0 0 12px;
}
.plugin-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}
</style>
