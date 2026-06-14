<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row page-header-row--actions-end">
        <p class="page-desc">Gateway 模型 SSOT、LiteLLM 同步与批量 Import 预览</p>
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="reloadAll">刷新</el-button>
      </div>
    </div>

    <el-row v-loading="loading" :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-card-value">{{ overview?.gatewayModels?.total ?? 0 }}</div>
          <div class="stat-card-label">Gateway 模型</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card stat-card--success">
          <div class="stat-card-value">{{ overview?.gatewayModels?.synced ?? 0 }}</div>
          <div class="stat-card-label">已同步</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-card-value">{{ overview?.secretRefCount ?? 0 }}</div>
          <div class="stat-card-label">密钥引用</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card" :class="overview?.liteLLMReachable ? 'stat-card--success' : 'stat-card--danger'">
          <div class="stat-card-value">{{ overview?.liteLLMReachable ? 'UP' : 'DOWN' }}</div>
          <div class="stat-card-label">LiteLLM 网关</div>
        </div>
      </el-col>
    </el-row>

    <div v-loading="syncLoading" class="table-panel integration-panel">
      <div class="table-panel-header">
        <div>
          <h3 class="table-panel-title">LiteLLM 同步状态</h3>
          <p class="table-panel-subtitle">
            {{ syncStatus?.liteLLMBaseUrl || '—' }} ·
            已同步 {{ syncStatus?.synced ?? 0 }}/{{ syncStatus?.total ?? 0 }}
          </p>
        </div>
        <el-button type="primary" :loading="syncRunning" @click="runSync">触发全量同步</el-button>
      </div>
      <el-table :data="syncStatus?.models ?? []" stripe empty-text="暂无模型">
        <el-table-column prop="modelName" label="模型名" min-width="160" />
        <el-table-column prop="upstreamModel" label="上游模型" min-width="200" show-overflow-tooltip />
        <el-table-column prop="syncStatus" label="同步状态" width="120">
          <template #default="{ row }">
            <el-tag :type="syncTagType(row.syncStatus)" size="small">{{ row.syncStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastSyncAt" label="上次同步" width="170" />
      </el-table>
    </div>

    <el-tabs v-model="importTab" class="integration-tabs">
      <el-tab-pane label="Gateway 模型 Import 预览" name="gateway">
        <div class="toolbar">
          <el-input
            v-model="gatewayImportJson"
            type="textarea"
            :rows="6"
            placeholder='{"dryRun":true,"items":[{"modelName":"...","upstreamModel":"..."}]}'
          />
          <el-button type="primary" :loading="importLoading" @click="runGatewayDryRun">dryRun 预览</el-button>
        </div>
        <ImportResultPanel v-if="gatewayResult" :result="gatewayResult" />
      </el-tab-pane>
      <el-tab-pane label="Provider Presets Import 预览" name="presets">
        <div class="toolbar">
          <el-input
            v-model="presetImportJson"
            type="textarea"
            :rows="6"
            placeholder='{"dryRun":true,"items":[{"presetCode":"...","presetName":"..."}]}'
          />
          <el-button type="primary" :loading="importLoading" @click="runPresetDryRun">dryRun 预览</el-button>
        </div>
        <ImportResultPanel v-if="presetResult" :result="presetResult" />
      </el-tab-pane>
      <el-tab-pane label="Webhook 投递历史" name="webhook">
        <div class="toolbar">
          <el-input v-model="webhookTaskCode" placeholder="按 taskCode 筛选" clearable style="width: 220px" @keyup.enter="loadWebhookDeliveries" />
          <el-button type="primary" :icon="Refresh" @click="loadWebhookDeliveries">刷新</el-button>
        </div>
        <div v-loading="webhookLoading" class="table-panel">
          <el-table :data="webhookDeliveries" stripe empty-text="暂无 Webhook 投递记录">
            <el-table-column prop="createdAt" label="时间" width="170" />
            <el-table-column prop="taskCode" label="作业" width="120" />
            <el-table-column prop="profileVersion" label="版本" width="90" />
            <el-table-column prop="eventType" label="事件" min-width="160" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'SENT' ? 'success' : row.deadLetter ? 'danger' : 'warning'" size="small">
                  {{ row.deadLetter ? 'DLQ' : row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="attemptCount" label="重试" width="80">
              <template #default="{ row }">{{ row.attemptCount }}/{{ row.maxAttempts }}</template>
            </el-table-column>
            <el-table-column prop="lastError" label="最近错误" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.id && (row.deadLetter || row.status === 'FAILED')"
                  link
                  type="primary"
                  @click="retryWebhook(row.id)"
                >
                  重试
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="ops-pagination">
            <el-pagination
              v-model:current-page="webhookPage"
              v-model:page-size="webhookSize"
              :total="webhookTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @current-change="loadWebhookDeliveries"
              @size-change="loadWebhookDeliveries"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { defineComponent, h, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  adminApi,
  normalizePage,
  type IntegrationImportResultVO,
  type IntegrationOverviewVO,
  type IntegrationWebhookDeliveryVO,
  type LiteLLMSyncStatusVO
} from '../api/admin'

const ImportResultPanel = defineComponent({
  props: { result: { type: Object as () => IntegrationImportResultVO, required: true } },
  setup(props) {
    return () =>
      h('div', { class: 'import-result' }, [
        h('p', [
          props.result.dryRun ? 'dryRun 预览 · ' : '',
          `创建 ${props.result.created} · 更新 ${props.result.updated} · 跳过 ${props.result.skipped}`
        ]),
        props.result.errors?.length
          ? h('ul', { class: 'import-errors' }, props.result.errors.map((e) => h('li', e)))
          : null
      ])
  }
})

const loading = ref(false)
const syncLoading = ref(false)
const syncRunning = ref(false)
const importLoading = ref(false)
const webhookLoading = ref(false)
const overview = ref<IntegrationOverviewVO | null>(null)
const syncStatus = ref<LiteLLMSyncStatusVO | null>(null)
const importTab = ref('gateway')
const gatewayImportJson = ref(
  JSON.stringify(
    {
      dryRun: true,
      items: [{ modelName: 'deepseek-v4-flash', upstreamModel: 'deepseek/deepseek-v4-flash', apiKeySecretRef: 'deepseek-api-key' }]
    },
    null,
    2
  )
)
const presetImportJson = ref(
  JSON.stringify(
    {
      dryRun: true,
      items: [{ presetCode: 'litellm-default', presetName: 'LiteLLM 默认', providerType: 'litellm', authMode: 'API_KEY', configJson: '{}' }]
    },
    null,
    2
  )
)
const gatewayResult = ref<IntegrationImportResultVO | null>(null)
const presetResult = ref<IntegrationImportResultVO | null>(null)
const webhookTaskCode = ref('')
const webhookPage = ref(1)
const webhookSize = ref(20)
const webhookTotal = ref(0)
const webhookDeliveries = ref<IntegrationWebhookDeliveryVO[]>([])

function syncTagType(status?: string) {
  if (status === 'SYNCED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

async function loadOverview() {
  overview.value = await adminApi.getIntegrationOverview()
}

async function loadSyncStatus() {
  syncLoading.value = true
  try {
    syncStatus.value = await adminApi.getLiteLLMSyncStatus()
  } finally {
    syncLoading.value = false
  }
}

async function runSync() {
  syncRunning.value = true
  try {
    await adminApi.syncLiteLLM()
    ElMessage.success('LiteLLM 同步已触发')
    await loadSyncStatus()
    await loadOverview()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '同步失败')
  } finally {
    syncRunning.value = false
  }
}

async function runGatewayDryRun() {
  importLoading.value = true
  try {
    const body = JSON.parse(gatewayImportJson.value)
    body.dryRun = true
    gatewayResult.value = await adminApi.importGatewayModels(body)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : 'JSON 无效或请求失败')
  } finally {
    importLoading.value = false
  }
}

async function runPresetDryRun() {
  importLoading.value = true
  try {
    const body = JSON.parse(presetImportJson.value)
    body.dryRun = true
    presetResult.value = await adminApi.importProviderPresets(body)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : 'JSON 无效或请求失败')
  } finally {
    importLoading.value = false
  }
}

async function loadWebhookDeliveries() {
  webhookLoading.value = true
  try {
    const page = await adminApi.listIntegrationWebhookDeliveries(
      webhookTaskCode.value || undefined,
      webhookPage.value,
      webhookSize.value
    )
    const normalized = normalizePage(page, webhookPage.value, webhookSize.value)
    webhookDeliveries.value = normalized.records
    webhookTotal.value = normalized.total
  } finally {
    webhookLoading.value = false
  }
}

async function retryWebhook(id: number) {
  try {
    await adminApi.retryIntegrationWebhookDelivery(id)
    ElMessage.success('Webhook 重试已提交')
    await loadWebhookDeliveries()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '重试失败')
  }
}

async function reloadAll() {
  loading.value = true
  try {
    await Promise.all([loadOverview(), loadSyncStatus(), loadWebhookDeliveries()])
  } finally {
    loading.value = false
  }
}

onMounted(reloadAll)
</script>

<style scoped>
.integration-panel {
  margin-bottom: 16px;
}

.integration-tabs {
  margin-top: 8px;
}

.toolbar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.import-result {
  padding: 12px 16px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  font-size: 14px;
}

.import-errors {
  margin: 8px 0 0;
  padding-left: 20px;
  color: var(--el-color-danger);
}

.ops-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
