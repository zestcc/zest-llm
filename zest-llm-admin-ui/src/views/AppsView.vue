<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row">
        <p class="page-desc">管理业务应用接入凭证与调用配额</p>
        <el-button type="primary" @click="openCreate">新建应用</el-button>
      </div>
    </div>

    <div v-loading="loading" class="table-panel">
      <div class="table-panel-header">
        <div>
          <h3 class="table-panel-title">已注册应用</h3>
          <p class="table-panel-subtitle">共 {{ total }} 个</p>
        </div>
      </div>
      <el-table :data="apps" stripe empty-text="暂无应用">
        <el-table-column prop="appKey" label="App Key" min-width="160">
          <template #default="{ row }">
            <span class="code-link">{{ row.appKey }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="appName" label="名称" min-width="140" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : row.status || '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" class="action-btn" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" class="action-btn" @click="openQuota(row)">配额</el-button>
            <el-button link type="warning" class="action-btn" @click="handleRotateToken(row)">轮换 Token</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="page-pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="onSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="createVisible" title="新建应用" width="480px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="appRules" label-width="90px">
        <el-form-item label="App Key" prop="appKey">
          <el-input v-model="createForm.appKey" placeholder="如 order-service" />
        </el-form-item>
        <el-form-item label="名称" prop="appName">
          <el-input v-model="createForm.appName" placeholder="应用显示名称" />
        </el-form-item>
        <el-form-item label="租户编码">
          <el-input v-model="createForm.tenantCode" placeholder="可选，默认 zest-demo" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑应用" width="480px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px">
        <el-form-item label="App Key">
          <el-input :model-value="editForm.appKey" disabled />
        </el-form-item>
        <el-form-item label="名称" prop="appName">
          <el-input v-model="editForm.appName" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="tokenVisible" title="新 Token（仅显示一次）" width="520px" :close-on-click-modal="false">
      <el-alert type="warning" show-icon :closable="false" title="请立即复制保存，关闭后将无法再次查看完整 Token。" />
      <div class="token-box">
        <code>{{ rotatedToken }}</code>
        <el-button type="primary" link @click="copyToken">复制</el-button>
      </div>
      <template #footer>
        <el-button type="primary" @click="tokenVisible = false">我已保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="quotaDrawer" :title="`配额 · ${quotaAppKey}`" size="420px">
      <el-form v-loading="quotaLoading" :model="quotaForm" label-width="120px">
        <el-form-item label="日 Token 上限">
          <el-input-number v-model="quotaForm.dailyTokenLimit" :min="0" :step="10000" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="QPS 上限">
          <el-input-number v-model="quotaForm.qpsLimit" :min="0" :step="10" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="日成本上限">
          <el-input-number v-model="quotaForm.dailyCostLimit" :min="0" :precision="2" :step="10" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="告警 Webhook">
          <el-input v-model="quotaForm.alertWebhookUrl" placeholder="https://hooks.example.com/cost" />
        </el-form-item>
        <el-form-item label="告警阈值(%)">
          <el-input-number v-model="quotaForm.alertThresholdPct" :min="1" :max="100" controls-position="right" style="width: 100%" />
        </el-form-item>
        <p class="quota-hint">
          成本告警记录可在
          <router-link :to="{ path: '/ops', query: { tab: 'cost' } }">运维中心 · 成本告警</router-link>
          查看
        </p>
        <el-form-item>
          <el-button type="primary" :loading="quotaSaving" @click="saveQuota">保存配额</el-button>
        </el-form-item>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { adminApi, normalizePage, type AppVO, type QuotaVO } from '../api/admin'

const loading = ref(false)
const submitting = ref(false)
const apps = ref<AppVO[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const createVisible = ref(false)
const editVisible = ref(false)
const tokenVisible = ref(false)
const rotatedToken = ref('')

const createFormRef = ref<FormInstance>()
const editFormRef = ref<FormInstance>()

const createForm = reactive({ appKey: '', appName: '', tenantCode: '' })
const editForm = reactive({ appKey: '', appName: '', status: 'ACTIVE' })

const quotaDrawer = ref(false)
const quotaAppKey = ref('')
const quotaLoading = ref(false)
const quotaSaving = ref(false)
const quotaForm = reactive<QuotaVO>({
  dailyTokenLimit: null,
  qpsLimit: null,
  dailyCostLimit: null,
  alertWebhookUrl: '',
  alertThresholdPct: 80
})

const appRules: FormRules = {
  appKey: [{ required: true, message: '请输入 App Key', trigger: 'blur' }],
  appName: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const editRules: FormRules = {
  appName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

async function load() {
  loading.value = true
  try {
    const data = await adminApi.listApps(page.value, pageSize.value)
    const pageData = normalizePage(data, page.value, pageSize.value)
    apps.value = pageData.records
    total.value = pageData.total
  } finally {
    loading.value = false
  }
}

function onSizeChange() {
  page.value = 1
  load()
}

function openCreate() {
  createForm.appKey = ''
  createForm.appName = ''
  createForm.tenantCode = ''
  createVisible.value = true
}

function openEdit(row: AppVO) {
  editForm.appKey = row.appKey
  editForm.appName = row.appName
  editForm.status = row.status || 'ACTIVE'
  editVisible.value = true
}

async function submitCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await adminApi.createApp({ ...createForm })
    ElMessage.success('应用创建成功')
    createVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function submitEdit() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await adminApi.updateApp(editForm.appKey, {
      appName: editForm.appName,
      status: editForm.status
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleRotateToken(row: AppVO) {
  await ElMessageBox.confirm(`确认轮换应用 ${row.appKey} 的访问 Token？旧 Token 将立即失效。`, '轮换 Token', {
    type: 'warning'
  })
  try {
    const result = await adminApi.rotateToken(row.appKey)
    rotatedToken.value = result.appToken
    tokenVisible.value = true
  } catch {
    /* handled by interceptor */
  }
}

function copyToken() {
  navigator.clipboard.writeText(rotatedToken.value).then(() => ElMessage.success('已复制到剪贴板'))
}

async function openQuota(row: AppVO) {
  quotaAppKey.value = row.appKey
  quotaDrawer.value = true
  quotaLoading.value = true
  try {
    const data = await adminApi.getQuota(row.appKey)
    quotaForm.dailyTokenLimit = data.dailyTokenLimit ?? null
    quotaForm.qpsLimit = data.qpsLimit ?? null
    quotaForm.dailyCostLimit = data.dailyCostLimit ?? null
    quotaForm.alertWebhookUrl = data.alertWebhookUrl ?? ''
    quotaForm.alertThresholdPct = data.alertThresholdPct ?? 80
  } finally {
    quotaLoading.value = false
  }
}

async function saveQuota() {
  quotaSaving.value = true
  try {
    await adminApi.updateQuota(quotaAppKey.value, { ...quotaForm })
    ElMessage.success('配额已更新')
  } finally {
    quotaSaving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.token-box {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-top: 16px;
  padding: 14px;
  background: var(--surface-muted);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
}

.token-box code {
  flex: 1;
  word-break: break-all;
  font-family: ui-monospace, monospace;
  font-size: 13px;
  color: #667eea;
}
.quota-hint {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--text-secondary, #666);
}
</style>
