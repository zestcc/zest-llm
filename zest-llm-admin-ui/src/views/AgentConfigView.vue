<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row">
        <p class="page-desc">智能体 Profile、Provider 预设与 Auth 绑定（对标 CC Switch 灵活配置）</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="config-tabs">
      <el-tab-pane label="智能体 Profile" name="profiles">
        <div class="toolbar">
          <el-select v-model="selectedTask" placeholder="选择 AI 作业" filterable style="width: 220px" @change="loadProfiles">
            <el-option v-for="t in tasks" :key="t.code" :label="`${t.code} · ${t.name}`" :value="t.code" />
          </el-select>
          <el-button type="success" :disabled="!selectedTask" :loading="probeLoading" @click="probePublished(false)">
            检测已发布
          </el-button>
          <el-button :disabled="!selectedTask" :loading="probeLoading" @click="probePublished(true)">
            冒烟测试
          </el-button>
          <el-button type="primary" :disabled="!selectedTask" @click="openCreateProfile">新建版本</el-button>
          <el-button :disabled="!selectedTask" @click="openImport">导入 JSON</el-button>
          <el-button :disabled="!selectedTask" @click="openProbeHistory">探测历史</el-button>
        </div>

        <el-table v-loading="profileLoading" :data="profiles" stripe empty-text="请选择作业或暂无 Profile">
          <el-table-column prop="version" label="版本" width="100" />
          <el-table-column prop="runtimeMode" label="运行时" width="90" />
          <el-table-column prop="providerPresetCode" label="Provider" min-width="140" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="可用性" width="110">
            <template #default="{ row }">
              <el-tag v-if="probeStatusMap[row.version]" :type="statusTagType(probeStatusMap[row.version])" size="small">
                {{ probeStatusMap[row.version] }}
              </el-tag>
              <span v-else class="probe-unknown">未检测</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="560" fixed="right">
            <template #default="{ row }">
              <el-button link type="success" @click="probeVersion(row, false)">检测</el-button>
              <el-button link type="warning" @click="probeVersion(row, true)">冒烟</el-button>
              <el-button link type="primary" @click="openEditProfile(row)">编辑</el-button>
              <el-button link type="primary" @click="exportProfile(row)">导出</el-button>
              <el-button v-if="row.status !== 'PUBLISHED'" link type="success" @click="publishProfile(row)">发布</el-button>
              <el-button
                v-if="row.status !== 'PUBLISHED' && row.publishedAt"
                link
                type="warning"
                @click="rollbackProfile(row)"
              >
                回滚
              </el-button>
              <el-button link type="info" @click="openProbeHistoryFor(row.version)">历史</el-button>
              <el-button
                v-if="publishedProfileVersion && publishedProfileVersion !== row.version"
                link
                type="info"
                @click="compareProfile(row.version)"
              >
                对比
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="presets.length" class="provider-switch-panel">
          <h4>一键切换 Provider</h4>
          <el-space wrap>
            <el-button
              v-for="p in presets"
              :key="p.presetCode"
              :type="activeProvider === p.presetCode ? 'primary' : 'default'"
              @click="activateProvider(p.presetCode)"
            >
              {{ p.presetName }}
            </el-button>
          </el-space>
        </div>
      </el-tab-pane>

      <el-tab-pane label="Provider 预设" name="presets">
        <div class="toolbar">
          <el-button type="primary" @click="openCreatePreset">新建预设</el-button>
        </div>
        <el-table v-loading="presetLoading" :data="presets" stripe>
          <el-table-column prop="presetCode" label="编码" min-width="140" />
          <el-table-column prop="presetName" label="名称" min-width="160" />
          <el-table-column prop="providerType" label="类型" width="100" />
          <el-table-column prop="authMode" label="Auth" width="110" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEditPreset(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="MCP Server" name="mcp">
        <div class="toolbar">
          <el-button type="primary" @click="openCreateMcp">新建 MCP Server</el-button>
        </div>
        <el-table v-loading="mcpLoading" :data="mcpServers" stripe empty-text="暂无 MCP Server">
          <el-table-column prop="serverCode" label="编码" min-width="140" />
          <el-table-column prop="serverName" label="名称" min-width="160" />
          <el-table-column prop="baseUrl" label="Base URL" min-width="220" />
          <el-table-column prop="authSecretRef" label="Auth Ref" min-width="160" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEditMcp(row)">编辑</el-button>
              <el-button link type="info" @click="probeMcpTools(row)">探测工具</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="Auth 绑定" name="auth">
        <div class="toolbar">
          <el-select v-model="authAppKey" placeholder="选择应用" filterable style="width: 220px" @change="loadAuth">
            <el-option v-for="a in apps" :key="a.appKey" :label="a.appKey" :value="a.appKey" />
          </el-select>
        </div>
        <el-form v-if="authForm" label-width="120px" class="auth-form">
          <el-form-item label="入站模式">
            <el-select v-model="authForm.inboundMode" style="width: 240px">
              <el-option label="静态 Token" value="STATIC_TOKEN" />
              <el-option label="OIDC JWT" value="OIDC_JWT" />
              <el-option label="API Key" value="API_KEY" />
            </el-select>
          </el-form-item>
          <el-form-item label="入站 JSON">
            <el-input v-model="authForm.inboundConfigJson" type="textarea" :rows="6" placeholder='{"mode":"OIDC_JWT","issuer":"https://...","audience":"zest-llm","jwksUri":"..."}' />
          </el-form-item>
          <el-form-item label="出站模式">
            <el-select v-model="authForm.outboundMode" style="width: 240px">
              <el-option label="API Key 引用" value="API_KEY_REF" />
              <el-option label="无" value="NONE" />
            </el-select>
          </el-form-item>
          <el-form-item label="出站 JSON">
            <el-input v-model="authForm.outboundConfigJson" type="textarea" :rows="4" placeholder='{"mode":"API_KEY_REF","secretRef":"env:LITELLM_API_KEY"}' />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="authSaving" @click="saveAuth">保存</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="profileDialogVisible" :title="profileDialogTitle" width="720px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item v-if="profileDialogMode === 'create'" label="版本">
          <el-input v-model="profileForm.version" placeholder="v2" />
        </el-form-item>
        <el-form-item label="Provider">
          <el-select v-model="profileForm.providerPresetCode" filterable style="width: 100%">
            <el-option v-for="p in presets" :key="p.presetCode" :label="p.presetName" :value="p.presetCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="运行时">
          <el-radio-group v-model="profileForm.runtimeMode">
            <el-radio value="invoke">invoke</el-radio>
            <el-radio value="agent">agent</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="Profile JSON">
          <el-input v-model="profileForm.profileJson" type="textarea" :rows="16" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="profileSaving" @click="submitProfile">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="presetDialogVisible" :title="presetDialogTitle" width="640px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item v-if="presetDialogMode === 'create'" label="编码">
          <el-input v-model="presetForm.presetCode" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="presetForm.presetName" />
        </el-form-item>
        <el-form-item label="类型">
          <el-input v-model="presetForm.providerType" />
        </el-form-item>
        <el-form-item label="Auth">
          <el-input v-model="presetForm.authMode" />
        </el-form-item>
        <el-form-item label="config JSON">
          <el-input v-model="presetForm.configJson" type="textarea" :rows="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="presetDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="presetSaving" @click="submitPreset">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="mcpDialogVisible" :title="mcpDialogTitle" width="640px" destroy-on-close>
      <el-form label-width="110px">
        <el-form-item v-if="mcpDialogMode === 'create'" label="编码">
          <el-input v-model="mcpForm.serverCode" placeholder="internal-docs" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="mcpForm.serverName" />
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input v-model="mcpForm.baseUrl" placeholder="http://localhost:9090/mcp" />
        </el-form-item>
        <el-form-item label="Auth Ref">
          <el-input v-model="mcpForm.authSecretRef" placeholder="env:MCP_TOKEN" />
        </el-form-item>
        <el-form-item label="config JSON">
          <el-input v-model="mcpForm.configJson" type="textarea" :rows="6" placeholder="{}" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mcpDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="mcpSaving" @click="submitMcp">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="导入 Profile JSON" width="640px" destroy-on-close>
      <el-input v-model="importJson" type="textarea" :rows="14" placeholder="粘贴完整 Profile JSON" />
      <el-checkbox v-model="importPublish" style="margin-top: 12px">导入后立即发布</el-checkbox>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importSaving" @click="submitImport">导入</el-button>
      </template>
    </el-dialog>
    <VersionDiffDialog ref="profileDiffDialogRef" />

    <el-dialog v-model="probeVisible" title="智能体可用性检测" width="760px" destroy-on-close>
      <div v-if="probeResult" class="probe-summary">
        <el-tag :type="statusTagType(probeResult.overallStatus)" size="large">
          {{ probeResult.overallStatus }}
        </el-tag>
        <span class="probe-meta">
          {{ probeResult.taskCode }} · {{ probeResult.profileVersion }} · {{ probeResult.latencyMs }}ms
        </span>
        <el-checkbox v-model="probeSmokeTest" style="margin-left: 12px" @change="rerunProbe">
          包含网关冒烟（消耗 token）
        </el-checkbox>
      </div>
      <el-table v-loading="probeLoading" :data="probeResult?.checks || []" stripe empty-text="暂无检测项">
        <el-table-column prop="name" label="检查项" min-width="160" />
        <el-table-column prop="category" label="类别" width="110" />
        <el-table-column label="关键" width="70">
          <template #default="{ row }">{{ row.critical ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.up ? 'success' : 'danger'" size="small">{{ row.up ? 'PASS' : 'FAIL' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="说明" min-width="240" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button @click="probeVisible = false">关闭</el-button>
        <el-button type="primary" :loading="probeLoading" @click="rerunProbe">重新检测</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="probeHistoryVisible" title="探测历史" width="860px" destroy-on-close>
      <div v-if="selectedTask" class="probe-history-meta">
        作业 <strong>{{ selectedTask }}</strong>
        <span v-if="probeHistoryVersion"> · 版本 {{ probeHistoryVersion }}</span>
      </div>
      <el-table v-loading="probeHistoryLoading" :data="filteredProbeHistory" stripe empty-text="暂无探测记录">
        <el-table-column prop="probedAt" label="时间" width="170" />
        <el-table-column prop="profileVersion" label="版本" width="90" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.overallStatus)" size="small">{{ row.overallStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="latencyMs" label="耗时(ms)" width="100" />
        <el-table-column prop="probeSource" label="来源" width="110" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="showProbeHistoryDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="probe-history-pagination">
        <el-pagination
          v-model:current-page="probeHistoryPage"
          v-model:page-size="probeHistorySize"
          :total="probeHistoryTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadProbeHistory"
          @size-change="loadProbeHistory"
        />
      </div>
      <template #footer>
        <el-button @click="probeHistoryVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import VersionDiffDialog from '../components/VersionDiffDialog.vue'
import { adminApi, normalizePage, type AppVO, type AgentProfileProbeResultVO, type AgentProfileVO, type AuthBindingVO, type McpServerVO, type ProviderPresetVO, type TaskVO } from '../api/admin'

const route = useRoute()
const activeTab = ref('profiles')
const tasks = ref<TaskVO[]>([])
const apps = ref<AppVO[]>([])
const selectedTask = ref('')
const profiles = ref<AgentProfileVO[]>([])
const profileLoading = ref(false)
const presets = ref<ProviderPresetVO[]>([])
const presetLoading = ref(false)
const activeProvider = ref('')

const profileDialogVisible = ref(false)
const profileDialogTitle = ref('')
const profileDialogMode = ref<'create' | 'edit'>('create')
const profileSaving = ref(false)
const profileForm = ref({
  version: '',
  providerPresetCode: 'litellm-default',
  runtimeMode: 'agent',
  profileJson: defaultProfileJson()
})

const presetDialogVisible = ref(false)
const presetDialogTitle = ref('')
const presetDialogMode = ref<'create' | 'edit'>('create')
const presetSaving = ref(false)
const presetForm = ref({
  presetCode: '',
  presetName: '',
  providerType: 'litellm',
  authMode: 'API_KEY',
  configJson: '{"type":"litellm","baseUrl":"http://localhost:4000","protocol":"openai"}'
})

const authAppKey = ref('')
const authForm = ref<AuthBindingVO | null>(null)
const authSaving = ref(false)

const importVisible = ref(false)
const importJson = ref('')
const importPublish = ref(true)
const importSaving = ref(false)
const profileDiffDialogRef = ref<InstanceType<typeof VersionDiffDialog> | null>(null)
const publishedProfileVersion = computed(() => profiles.value.find((p) => p.status === 'PUBLISHED')?.version)

const probeVisible = ref(false)
const probeLoading = ref(false)
const probeResult = ref<AgentProfileProbeResultVO | null>(null)
const probeSmokeTest = ref(false)
const probeTargetVersion = ref<string | null>(null)
const probeStatusMap = ref<Record<string, string>>({})

const probeHistoryVisible = ref(false)
const probeHistoryLoading = ref(false)
const probeHistoryRecords = ref<AgentProfileProbeResultVO[]>([])
const probeHistoryPage = ref(1)
const probeHistorySize = ref(20)
const probeHistoryTotal = ref(0)
const probeHistoryVersion = ref<string | null>(null)

const filteredProbeHistory = computed(() => {
  if (!probeHistoryVersion.value) return probeHistoryRecords.value
  return probeHistoryRecords.value.filter((r) => r.profileVersion === probeHistoryVersion.value)
})

const mcpServers = ref<McpServerVO[]>([])
const mcpLoading = ref(false)
const mcpDialogVisible = ref(false)
const mcpDialogTitle = ref('')
const mcpDialogMode = ref<'create' | 'edit'>('create')
const mcpSaving = ref(false)
const mcpForm = ref({
  serverCode: '',
  serverName: '',
  baseUrl: '',
  authSecretRef: '',
  configJson: '{}'
})

function defaultProfileJson() {
  return JSON.stringify(
    {
      apiVersion: 'zestllm/v1',
      runtimeMode: 'agent',
      providerRef: 'litellm-default',
      model: { primary: 'gpt-4o-mini', fallback: ['gpt-3.5-turbo'] },
      toolCallMode: 'loop',
      generation: { maxTokens: 1024, temperature: 0.7, timeoutMs: 30000, maxToolSteps: 3 },
      tools: [],
      guardrails: { piiRedact: false, blockOnSchemaMismatch: true },
      inboundAuth: { mode: 'STATIC_TOKEN' },
      outboundAuth: { mode: 'API_KEY_REF', secretRef: 'env:LITELLM_API_KEY' }
    },
    null,
    2
  )
}

async function loadTasks() {
  const data = await adminApi.listTasks(1, 200)
  tasks.value = normalizePage(data, 1, 200).records
  if (!selectedTask.value && tasks.value.length) {
    selectedTask.value = tasks.value[0].code
    await loadProfiles()
  }
}

async function loadApps() {
  const data = await adminApi.listApps(1, 200)
  apps.value = normalizePage(data, 1, 200).records
}

async function loadPresets() {
  presetLoading.value = true
  try {
    presets.value = await adminApi.listProviderPresets()
  } finally {
    presetLoading.value = false
  }
}

async function loadProfiles() {
  if (!selectedTask.value) return
  profileLoading.value = true
  try {
    profiles.value = await adminApi.listAgentProfiles(selectedTask.value)
    const published = profiles.value.find((p) => p.status === 'PUBLISHED')
    activeProvider.value = published?.providerPresetCode || ''
    await loadLatestProbeStatus()
  } finally {
    profileLoading.value = false
  }
}

async function loadLatestProbeStatus() {
  if (!selectedTask.value) return
  try {
    const latest = await adminApi.getAgentProfileProbeLatest(selectedTask.value)
    if (latest?.profileVersion && latest.overallStatus) {
      probeStatusMap.value = {
        ...probeStatusMap.value,
        [latest.profileVersion]: latest.overallStatus
      }
    }
  } catch {
    /* ignore */
  }
}

function openCreateProfile() {
  profileDialogMode.value = 'create'
  profileDialogTitle.value = '新建 Profile 版本'
  profileForm.value = {
    version: `v${profiles.value.length + 1}`,
    providerPresetCode: activeProvider.value || 'litellm-default',
    runtimeMode: 'agent',
    profileJson: defaultProfileJson()
  }
  profileDialogVisible.value = true
}

function openEditProfile(row: AgentProfileVO) {
  profileDialogMode.value = 'edit'
  profileDialogTitle.value = `编辑 ${row.version}`
  profileForm.value = {
    version: row.version,
    providerPresetCode: row.providerPresetCode || 'litellm-default',
    runtimeMode: row.runtimeMode || 'agent',
    profileJson: row.profileJson || defaultProfileJson()
  }
  profileDialogVisible.value = true
}

async function submitProfile() {
  if (!selectedTask.value) return
  profileSaving.value = true
  try {
    if (profileDialogMode.value === 'create') {
      await adminApi.createAgentProfile(selectedTask.value, {
        version: profileForm.value.version,
        profileJson: profileForm.value.profileJson,
        providerPresetCode: profileForm.value.providerPresetCode,
        runtimeMode: profileForm.value.runtimeMode
      })
    } else {
      await adminApi.updateAgentProfile(selectedTask.value, profileForm.value.version, {
        profileJson: profileForm.value.profileJson,
        providerPresetCode: profileForm.value.providerPresetCode,
        runtimeMode: profileForm.value.runtimeMode
      })
    }
    profileDialogVisible.value = false
    ElMessage.success('保存成功')
    await loadProfiles()
  } finally {
    profileSaving.value = false
  }
}

async function publishProfile(row: AgentProfileVO) {
  await adminApi.publishAgentProfile(selectedTask.value, row.version)
  ElMessage.success('已发布')
  await loadProfiles()
}

async function rollbackProfile(row: AgentProfileVO) {
  await adminApi.rollbackAgentProfile(selectedTask.value, row.version)
  ElMessage.success(`已回滚至 ${row.version}`)
  await loadProfiles()
}

function compareProfile(version: string) {
  const published = publishedProfileVersion.value
  if (!published || !profileDiffDialogRef.value || !selectedTask.value) return
  profileDiffDialogRef.value.open({
    title: `Profile 对比 · ${published} → ${version}`,
    loader: () => adminApi.diffAgentProfile(selectedTask.value, published, version)
  })
}

async function exportProfile(row: AgentProfileVO) {
  const data = await adminApi.exportAgentProfile(selectedTask.value, row.version)
  await navigator.clipboard.writeText(data.profileJson)
  ElMessage.success('已复制到剪贴板')
}

async function activateProvider(code: string) {
  if (!selectedTask.value) return
  await adminApi.activateAgentProvider(selectedTask.value, code)
  activeProvider.value = code
  ElMessage.success(`已切换 Provider: ${code}`)
  await loadProfiles()
}

function openCreatePreset() {
  presetDialogMode.value = 'create'
  presetDialogTitle.value = '新建 Provider 预设'
  presetForm.value = {
    presetCode: '',
    presetName: '',
    providerType: 'litellm',
    authMode: 'API_KEY',
    configJson: '{"type":"litellm","baseUrl":"http://localhost:4000","protocol":"openai"}'
  }
  presetDialogVisible.value = true
}

function openEditPreset(row: ProviderPresetVO) {
  presetDialogMode.value = 'edit'
  presetDialogTitle.value = `编辑 ${row.presetCode}`
  presetForm.value = {
    presetCode: row.presetCode,
    presetName: row.presetName,
    providerType: row.providerType || 'litellm',
    authMode: row.authMode || 'API_KEY',
    configJson: row.configJson || '{}'
  }
  presetDialogVisible.value = true
}

async function submitPreset() {
  presetSaving.value = true
  try {
    if (presetDialogMode.value === 'create') {
      await adminApi.createProviderPreset(presetForm.value)
    } else {
      await adminApi.updateProviderPreset(presetForm.value.presetCode, presetForm.value)
    }
    presetDialogVisible.value = false
    ElMessage.success('保存成功')
    await loadPresets()
  } finally {
    presetSaving.value = false
  }
}

async function loadAuth() {
  if (!authAppKey.value) return
  authForm.value = await adminApi.getAuthBinding(authAppKey.value)
  if (!authForm.value.inboundConfigJson) {
    authForm.value.inboundConfigJson = JSON.stringify({ mode: authForm.value.inboundMode || 'STATIC_TOKEN' }, null, 2)
  }
}

async function saveAuth() {
  if (!authForm.value || !authAppKey.value) return
  authSaving.value = true
  try {
    await adminApi.upsertAuthBinding({
      scopeType: 'APP',
      appKey: authAppKey.value,
      inboundMode: authForm.value.inboundMode || 'STATIC_TOKEN',
      inboundConfigJson: authForm.value.inboundConfigJson,
      outboundMode: authForm.value.outboundMode,
      outboundConfigJson: authForm.value.outboundConfigJson
    })
    ElMessage.success('Auth 已保存')
  } finally {
    authSaving.value = false
  }
}

function openImport() {
  importJson.value = defaultProfileJson()
  importVisible.value = true
}

async function submitImport() {
  if (!selectedTask.value) return
  importSaving.value = true
  try {
    await adminApi.importAgentProfile({
      taskCode: selectedTask.value,
      profileJson: importJson.value,
      publish: importPublish.value
    })
    importVisible.value = false
    ElMessage.success('导入成功')
    await loadProfiles()
  } finally {
    importSaving.value = false
  }
}

async function loadMcpServers() {
  mcpLoading.value = true
  try {
    mcpServers.value = await adminApi.listMcpServers()
  } finally {
    mcpLoading.value = false
  }
}

function openCreateMcp() {
  mcpDialogMode.value = 'create'
  mcpDialogTitle.value = '新建 MCP Server'
  mcpForm.value = {
    serverCode: '',
    serverName: '',
    baseUrl: 'http://localhost:9090/mcp',
    authSecretRef: '',
    configJson: '{}'
  }
  mcpDialogVisible.value = true
}

function openEditMcp(row: McpServerVO) {
  mcpDialogMode.value = 'edit'
  mcpDialogTitle.value = `编辑 ${row.serverCode}`
  mcpForm.value = {
    serverCode: row.serverCode,
    serverName: row.serverName,
    baseUrl: row.baseUrl,
    authSecretRef: row.authSecretRef || '',
    configJson: row.configJson || '{}'
  }
  mcpDialogVisible.value = true
}

async function submitMcp() {
  mcpSaving.value = true
  try {
    if (mcpDialogMode.value === 'create') {
      await adminApi.createMcpServer(mcpForm.value)
    } else {
      await adminApi.updateMcpServer(mcpForm.value.serverCode, {
        serverName: mcpForm.value.serverName,
        baseUrl: mcpForm.value.baseUrl,
        authSecretRef: mcpForm.value.authSecretRef,
        configJson: mcpForm.value.configJson
      })
    }
    mcpDialogVisible.value = false
    ElMessage.success('保存成功')
    await loadMcpServers()
  } finally {
    mcpSaving.value = false
  }
}

async function probeMcpTools(row: McpServerVO) {
  const tools = await adminApi.listMcpServerTools(row.serverCode)
  ElMessage.info(tools.length ? `发现 ${tools.length} 个工具: ${tools.map((t) => t.name).join(', ')}` : '未发现远程工具（MCP Server 可能离线）')
}

function statusTagType(status?: string) {
  if (status === 'READY') return 'success'
  if (status === 'DEGRADED') return 'warning'
  if (status === 'UNAVAILABLE') return 'danger'
  return 'info'
}

async function probePublished(smokeTest: boolean) {
  if (!selectedTask.value) return
  probeTargetVersion.value = publishedProfileVersion.value || null
  probeSmokeTest.value = smokeTest
  await runProbe(async () => adminApi.probeAgentProfilePublished(selectedTask.value, { smokeTest }))
}

async function probeVersion(row: AgentProfileVO, smokeTest: boolean) {
  if (!selectedTask.value) return
  probeTargetVersion.value = row.version
  probeSmokeTest.value = smokeTest
  await runProbe(async () => adminApi.probeAgentProfileVersion(selectedTask.value, row.version, { smokeTest }))
}

async function runProbe(loader: () => Promise<AgentProfileProbeResultVO>) {
  probeLoading.value = true
  probeVisible.value = true
  try {
    probeResult.value = await loader()
    if (probeResult.value.profileVersion) {
      probeStatusMap.value = {
        ...probeStatusMap.value,
        [probeResult.value.profileVersion]: probeResult.value.overallStatus || 'UNKNOWN'
      }
    }
  } finally {
    probeLoading.value = false
  }
}

async function rerunProbe() {
  if (!selectedTask.value || !probeTargetVersion.value) return
  if (probeTargetVersion.value === publishedProfileVersion.value) {
    await probePublished(probeSmokeTest.value)
    return
  }
  const row = profiles.value.find((p) => p.version === probeTargetVersion.value)
  if (row) {
    await probeVersion(row, probeSmokeTest.value)
  }
}

function openProbeHistory() {
  probeHistoryVersion.value = null
  probeHistoryPage.value = 1
  probeHistoryVisible.value = true
  loadProbeHistory()
}

function openProbeHistoryFor(version: string) {
  probeHistoryVersion.value = version
  probeHistoryPage.value = 1
  probeHistoryVisible.value = true
  loadProbeHistory()
}

async function loadProbeHistory() {
  if (!selectedTask.value) return
  probeHistoryLoading.value = true
  try {
    const data = await adminApi.listAgentProfileProbeHistory(
      selectedTask.value,
      probeHistoryPage.value,
      probeHistorySize.value
    )
    probeHistoryRecords.value = data.records || []
    probeHistoryTotal.value = data.total ?? 0
  } finally {
    probeHistoryLoading.value = false
  }
}

function showProbeHistoryDetail(row: AgentProfileProbeResultVO) {
  probeResult.value = row
  probeTargetVersion.value = row.profileVersion || null
  probeSmokeTest.value = false
  probeVisible.value = true
}

onMounted(async () => {
  await Promise.all([loadTasks(), loadApps(), loadPresets(), loadMcpServers()])
  const taskFromQuery = route.query.task
  if (typeof taskFromQuery === 'string' && taskFromQuery) {
    selectedTask.value = taskFromQuery
    await loadProfiles()
  }
})

watch(
  () => route.query.task,
  async (taskCode) => {
    if (typeof taskCode === 'string' && taskCode && taskCode !== selectedTask.value) {
      selectedTask.value = taskCode
      await loadProfiles()
    }
  }
)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.config-tabs {
  background: var(--panel-bg, #fff);
  padding: 16px;
  border-radius: 12px;
}
.provider-switch-panel {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color, #eee);
}
.provider-switch-panel h4 {
  margin: 0 0 12px;
  font-size: 14px;
  color: var(--text-secondary, #666);
}
.auth-form {
  max-width: 720px;
  margin-top: 16px;
}
.probe-unknown {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.probe-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
.probe-meta {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.probe-history-meta {
  margin-bottom: 12px;
  color: var(--text-secondary, #666);
  font-size: 13px;
}
.probe-history-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
