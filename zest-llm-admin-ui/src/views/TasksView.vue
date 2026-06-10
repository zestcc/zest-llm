<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row">
        <p class="page-desc">管理 AI 作业定义，关联应用与 Prompt / 模型路由</p>
        <el-button type="primary" @click="openCreate">新建作业</el-button>
      </div>
    </div>

    <div v-loading="overviewLoading" class="table-panel" style="margin-bottom: 16px">
      <div class="table-panel-header">
        <h3 class="table-panel-title">作业看板（近 7 天）</h3>
      </div>
      <el-table :data="overview" stripe empty-text="暂无作业">
        <el-table-column prop="code" label="Code" width="120" />
        <el-table-column prop="name" label="名称" width="140" />
        <el-table-column prop="publishedVersion" label="已发布版本" width="120" />
        <el-table-column prop="probeStatus" label="Probe" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.probeStatus === 'READY' ? 'success' : row.probeStatus === 'DEGRADED' ? 'warning' : 'info'"
              size="small"
            >
              {{ row.probeStatus || 'UNKNOWN' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="executionsLast7d" label="执行次数" width="100" />
        <el-table-column prop="failedLast7d" label="失败" width="80" />
      </el-table>
    </div>

    <div v-loading="loading" class="table-panel">
      <div class="table-panel-header">
        <div>
          <h3 class="table-panel-title">AI 作业列表</h3>
          <p class="table-panel-subtitle">共 {{ tasks.length }} 个</p>
        </div>
      </div>
      <el-table :data="tasks" stripe empty-text="暂无作业">
        <el-table-column prop="code" label="Code" width="140">
          <template #default="{ row }">
            <span class="code-link">{{ row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="appKey" label="所属应用" width="140" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : row.status || '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" class="action-btn" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="createVisible" title="新建 AI 作业" width="520px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="taskRules" label-width="90px">
        <el-form-item label="所属应用" prop="appKey">
          <el-input v-model="createForm.appKey" placeholder="如 order-service" />
        </el-form-item>
        <el-form-item label="Code" prop="code">
          <el-input v-model="createForm.code" placeholder="如 aiChat" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="createForm.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑 AI 作业" width="520px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px">
        <el-form-item label="Code">
          <el-input :model-value="editForm.code" disabled />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { adminApi, normalizePage, type AiJobOverviewVO, type TaskVO } from '../api/admin'

const loading = ref(false)
const overviewLoading = ref(false)
const submitting = ref(false)
const tasks = ref<TaskVO[]>([])
const overview = ref<AiJobOverviewVO[]>([])

const createVisible = ref(false)
const editVisible = ref(false)
const createFormRef = ref<FormInstance>()
const editFormRef = ref<FormInstance>()

const createForm = reactive({
  appKey: 'order-service',
  code: '',
  name: '',
  description: ''
})

const editForm = reactive({
  id: 0,
  code: '',
  name: '',
  description: '',
  status: 'ACTIVE'
})

const taskRules: FormRules = {
  appKey: [{ required: true, message: '请输入所属应用', trigger: 'blur' }],
  code: [{ required: true, message: '请输入 Code', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const editRules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

async function loadOverview() {
  overviewLoading.value = true
  try {
    const data = await adminApi.getAiJobOverview()
    overview.value = data.data ?? data ?? []
  } finally {
    overviewLoading.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const data = await adminApi.listTasks()
    const pageData = normalizePage(data, 1, 500)
    tasks.value = pageData.records
  } finally {
    loading.value = false
  }
}

function openCreate() {
  createForm.code = ''
  createForm.name = ''
  createForm.description = ''
  createVisible.value = true
}

function openEdit(row: TaskVO) {
  editForm.id = row.id
  editForm.code = row.code
  editForm.name = row.name
  editForm.description = row.description || ''
  editForm.status = row.status || 'ACTIVE'
  editVisible.value = true
}

async function submitCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await adminApi.createTask({ ...createForm })
    ElMessage.success('作业创建成功')
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
    await adminApi.updateTask(editForm.code, {
      name: editForm.name,
      description: editForm.description,
      status: editForm.status
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadOverview()
  load()
})
</script>
