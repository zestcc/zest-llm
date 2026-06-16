<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row">
        <div class="page-filters">
          <AppSelect
            v-model="filterAppKey"
            placeholder="筛选应用"
            clearable
            width="220px"
            select-class="page-filter-control"
          />
          <el-button type="primary" :loading="loading" @click="loadDatasets">刷新数据集</el-button>
          <el-button @click="createDemoDataset">新建数据集</el-button>
        </div>
      </div>
    </div>

    <div v-loading="loading" class="table-panel">
      <div class="table-panel-header">
        <h3 class="table-panel-title">Eval 数据集</h3>
      </div>
      <el-table :data="filteredDatasets" stripe empty-text="暂无数据集">
        <el-table-column prop="datasetCode" label="Code" width="160" />
        <el-table-column prop="datasetName" label="名称" min-width="180" />
        <el-table-column prop="appKey" label="App" width="140" />
        <el-table-column prop="taskCode" label="Task" width="120" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              :type="casesDataset === row.datasetCode ? 'warning' : 'primary'"
              @click="openCases(row.datasetCode)"
            >
              {{ casesDataset === row.datasetCode ? '收起用例' : '管理用例' }}
            </el-button>
            <el-button link type="primary" :loading="runningCode === row.datasetCode" @click="runEval(row.datasetCode)">
              运行 Eval
            </el-button>
            <el-button link type="info" @click="loadRuns(row.datasetCode)">历史</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="casesDataset" v-loading="casesLoading" class="table-panel" style="margin-top: 16px">
      <div class="table-panel-header">
        <div>
          <h3 class="table-panel-title">用例管理 · {{ casesDataset }}</h3>
          <p class="table-panel-subtitle">编辑 inputs / expected 后运行 Eval 验证</p>
        </div>
        <div class="table-panel-actions">
          <el-button type="primary" @click="openCreateCase">新建用例</el-button>
          <el-button @click="closeCases">收起</el-button>
        </div>
      </div>
      <el-table :data="cases" stripe empty-text="暂无用例，请先新建">
        <el-table-column prop="caseCode" label="Case Code" width="160" />
        <el-table-column label="Inputs" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ formatJson(row.inputs) }}</template>
        </el-table-column>
        <el-table-column label="Expected" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ formatJson(row.expected) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditCase(row)">编辑</el-button>
            <el-button link type="danger" @click="removeCase(row.caseCode)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="lastRun" class="table-panel" style="margin-top: 16px">
      <div class="table-panel-header">
        <h3 class="table-panel-title">最近运行 · {{ lastRun.runCode }}</h3>
        <p class="table-panel-subtitle">
          通过率 {{ lastRun.passRate }}% · {{ lastRun.passedCases }}/{{ lastRun.totalCases }} · {{ lastRun.status }}
        </p>
      </div>
      <el-progress
        v-if="lastRun.totalCases"
        :percentage="Math.round(lastRun.passRate || 0)"
        :status="lastRun.status === 'PASSED' ? 'success' : 'warning'"
        style="margin-bottom: 12px"
      />
      <el-table :data="failedCases" stripe empty-text="全部通过">
        <el-table-column prop="caseCode" label="失败 Case" width="140" />
        <el-table-column prop="reason" label="Reason" min-width="200" show-overflow-tooltip />
        <el-table-column prop="traceId" label="TraceId" min-width="200" show-overflow-tooltip />
      </el-table>
      <el-table :data="lastRun.caseResults || []" stripe style="margin-top: 12px">
        <el-table-column prop="caseCode" label="Case" width="140" />
        <el-table-column prop="status" label="Status" width="100" />
        <el-table-column label="Passed" width="90">
          <template #default="{ row }">
            <el-tag :type="row.passed ? 'success' : 'danger'" size="small">{{ row.passed ? 'YES' : 'NO' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="traceId" label="TraceId" min-width="200" show-overflow-tooltip />
        <el-table-column prop="reason" label="Reason" min-width="160" show-overflow-tooltip />
      </el-table>
    </div>

    <div v-if="runs.length" class="table-panel" style="margin-top: 16px">
      <div class="table-panel-header">
        <h3 class="table-panel-title">历史运行 · {{ selectedDataset }}</h3>
        <el-button @click="closeRuns">收起</el-button>
      </div>
      <el-table :data="runs" stripe>
        <el-table-column prop="runCode" label="Run" width="160" />
        <el-table-column prop="status" label="Status" width="100" />
        <el-table-column prop="passRate" label="通过率" width="100" />
        <el-table-column prop="passedCases" label="通过" width="80" />
        <el-table-column prop="failedCases" label="失败" width="80" />
        <el-table-column prop="startedAt" label="开始时间" min-width="170" />
      </el-table>
    </div>

    <el-dialog v-model="caseDialogVisible" :title="caseDialogTitle" width="640px" destroy-on-close>
      <el-form ref="caseFormRef" :model="caseForm" :rules="caseRules" label-width="110px">
        <el-form-item v-if="!editingCaseCode" label="Case Code" prop="caseCode">
          <el-input v-model="caseForm.caseCode" placeholder="如 case-hello" />
        </el-form-item>
        <el-form-item label="Inputs JSON" prop="inputsText">
          <el-input
            v-model="caseForm.inputsText"
            type="textarea"
            :rows="6"
            placeholder='{"question":"hello"}'
          />
        </el-form-item>
        <el-form-item label="Expected JSON" prop="expectedText">
          <el-input
            v-model="caseForm.expectedText"
            type="textarea"
            :rows="4"
            placeholder='{"status":"SUCCESS"} 或 {"answerContains":"关键词"}'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="caseDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="caseSaving" @click="saveCase">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { adminApi, type EvalCaseVO, type EvalDatasetVO, type EvalRunVO } from '../api/admin'
import AppSelect from '../components/AppSelect.vue'
import { getLastAppKey } from '../utils/lastAppKey'

const loading = ref(false)
const filterAppKey = ref(getLastAppKey())
const runningCode = ref('')
const datasets = ref<EvalDatasetVO[]>([])
const filteredDatasets = computed(() =>
  filterAppKey.value ? datasets.value.filter((d) => d.appKey === filterAppKey.value) : datasets.value
)
const runs = ref<EvalRunVO[]>([])
const lastRun = ref<EvalRunVO | null>(null)
const selectedDataset = ref('')

const casesDataset = ref('')
const casesLoading = ref(false)
const cases = ref<EvalCaseVO[]>([])

const caseDialogVisible = ref(false)
const caseDialogTitle = ref('新建用例')
const editingCaseCode = ref('')
const caseSaving = ref(false)
const caseFormRef = ref<FormInstance>()
const caseForm = reactive({
  caseCode: '',
  inputsText: '{"question":""}',
  expectedText: '{"status":"SUCCESS"}'
})

const caseRules: FormRules = {
  caseCode: [{ required: true, message: '请输入 Case Code', trigger: 'blur' }],
  inputsText: [{ required: true, validator: validateJsonField, trigger: 'blur' }],
  expectedText: [{ validator: validateOptionalJsonField, trigger: 'blur' }]
}

const failedCases = computed(() => (lastRun.value?.caseResults || []).filter((c) => !c.passed))

function validateJsonField(_rule: unknown, value: string, callback: (err?: Error) => void) {
  if (!value?.trim()) {
    callback(new Error('请输入 JSON'))
    return
  }
  try {
    JSON.parse(value)
    callback()
  } catch {
    callback(new Error('JSON 格式无效'))
  }
}

function validateOptionalJsonField(_rule: unknown, value: string, callback: (err?: Error) => void) {
  if (!value?.trim()) {
    callback()
    return
  }
  try {
    JSON.parse(value)
    callback()
  } catch {
    callback(new Error('JSON 格式无效'))
  }
}

function formatJson(value: Record<string, unknown> | undefined) {
  if (!value || !Object.keys(value).length) return '—'
  return JSON.stringify(value)
}

async function loadDatasets() {
  loading.value = true
  try {
    datasets.value = await adminApi.listEvalDatasets()
  } finally {
    loading.value = false
  }
}

async function openCases(datasetCode: string) {
  if (casesDataset.value === datasetCode) {
    closeCases()
    return
  }
  casesDataset.value = datasetCode
  await loadCases()
}

function closeCases() {
  casesDataset.value = ''
  cases.value = []
}

async function loadCases() {
  if (!casesDataset.value) return
  casesLoading.value = true
  try {
    cases.value = await adminApi.listEvalCases(casesDataset.value)
  } finally {
    casesLoading.value = false
  }
}

function openCreateCase() {
  editingCaseCode.value = ''
  caseDialogTitle.value = '新建用例'
  caseForm.caseCode = ''
  caseForm.inputsText = '{"question":""}'
  caseForm.expectedText = '{"status":"SUCCESS"}'
  caseDialogVisible.value = true
}

function openEditCase(row: EvalCaseVO) {
  editingCaseCode.value = row.caseCode
  caseDialogTitle.value = `编辑用例 · ${row.caseCode}`
  caseForm.caseCode = row.caseCode
  caseForm.inputsText = JSON.stringify(row.inputs || {}, null, 2)
  caseForm.expectedText = row.expected && Object.keys(row.expected).length
    ? JSON.stringify(row.expected, null, 2)
    : ''
  caseDialogVisible.value = true
}

async function saveCase() {
  const valid = await caseFormRef.value?.validate().catch(() => false)
  if (!valid || !casesDataset.value) return

  const inputs = JSON.parse(caseForm.inputsText) as Record<string, unknown>
  const expected = caseForm.expectedText.trim()
    ? (JSON.parse(caseForm.expectedText) as Record<string, unknown>)
    : undefined

  caseSaving.value = true
  try {
    if (editingCaseCode.value) {
      await adminApi.updateEvalCase(casesDataset.value, editingCaseCode.value, { inputs, expected })
      ElMessage.success('用例已更新')
    } else {
      await adminApi.createEvalCase(casesDataset.value, {
        caseCode: caseForm.caseCode.trim(),
        inputs,
        expected
      })
      ElMessage.success('用例已创建')
    }
    caseDialogVisible.value = false
    await loadCases()
  } finally {
    caseSaving.value = false
  }
}

async function removeCase(caseCode: string) {
  if (!casesDataset.value) return
  await ElMessageBox.confirm(`确定删除用例 ${caseCode}？`, '删除确认', { type: 'warning' })
  await adminApi.deleteEvalCase(casesDataset.value, caseCode)
  ElMessage.success('用例已删除')
  await loadCases()
}

async function runEval(datasetCode: string) {
  runningCode.value = datasetCode
  try {
    lastRun.value = await adminApi.runEvalDataset(datasetCode)
    ElMessage.success(`Eval 完成: ${lastRun.value.passRate}%`)
    await loadRuns(datasetCode)
  } finally {
    runningCode.value = ''
  }
}

async function loadRuns(datasetCode: string) {
  if (selectedDataset.value === datasetCode && runs.value.length) {
    closeRuns()
    return
  }
  selectedDataset.value = datasetCode
  runs.value = await adminApi.listEvalRuns(datasetCode)
}

function closeRuns() {
  selectedDataset.value = ''
  runs.value = []
}

async function createDemoDataset() {
  const code = `eval-${Date.now().toString().slice(-6)}`
  await adminApi.createEvalDataset({
    datasetCode: code,
    datasetName: `Quick ${code}`,
    appKey: getLastAppKey() || 'order-service',
    taskCode: 'aiChat'
  })
  ElMessage.success(`已创建数据集 ${code}`)
  await loadDatasets()
}

loadDatasets()
</script>

<style scoped>
.table-panel-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
