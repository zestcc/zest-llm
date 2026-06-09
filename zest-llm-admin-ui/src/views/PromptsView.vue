<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row">
        <div class="page-filters">
          <el-input
            v-model="code"
            placeholder="作业 code，如 aiChat"
            class="page-filter-control"
            clearable
            @keyup.enter="load"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="openCreate">新建版本</el-button>
        </div>
      </div>
    </div>

    <div v-loading="loading" class="table-panel">
      <div class="table-panel-header">
        <div>
          <h3 class="table-panel-title">Prompt 版本</h3>
          <p class="table-panel-subtitle">作业 code: {{ code }}</p>
        </div>
      </div>
      <el-table :data="versions" stripe empty-text="暂无版本">
        <el-table-column prop="version" label="版本" width="100">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.version }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'PUBLISHED' ? 'success' : row.status === 'DRAFT' ? 'warning' : 'info'"
              size="small"
            >
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="模板摘要" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ (row.templateBody || '').slice(0, 120) }}</template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PUBLISHED'">
              <el-tag size="small" type="success" effect="plain">当前</el-tag>
            </template>
            <template v-else>
              <el-button link type="primary" class="action-btn" @click="publish(row.version)">发布</el-button>
              <el-button
                v-if="row.publishedAt"
                link
                type="warning"
                class="action-btn"
                @click="rollback(row.version)"
              >
                回滚
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="createVisible" title="新建 Prompt 版本" width="720px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="版本号" prop="version">
          <el-input v-model="createForm.version" placeholder="如 v2" style="max-width: 200px" />
        </el-form-item>
        <el-form-item label="模板内容" prop="templateBody">
          <el-input
            v-model="createForm.templateBody"
            type="textarea"
            :rows="12"
            class="prompt-editor"
            placeholder="Handlebars 模板，如 {{question}}"
          />
        </el-form-item>
        <el-form-item label="输出 Schema">
          <el-input
            v-model="createForm.outputSchema"
            type="textarea"
            :rows="4"
            class="prompt-editor"
            placeholder='{"type":"object","properties":{...}}'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">创建草稿</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { adminApi, type PromptVersionVO } from '../api/admin'

const code = ref('aiChat')
const loading = ref(false)
const submitting = ref(false)
const versions = ref<PromptVersionVO[]>([])

const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = ref({
  version: '',
  templateBody: '',
  outputSchema: ''
})

const createRules: FormRules = {
  version: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  templateBody: [{ required: true, message: '请输入模板内容', trigger: 'blur' }]
}

async function load() {
  if (!code.value) return
  loading.value = true
  try {
    versions.value = await adminApi.listPromptVersions(code.value)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  if (!code.value) {
    ElMessage.warning('请先输入作业 code')
    return
  }
  createForm.value = { version: '', templateBody: '', outputSchema: '' }
  createVisible.value = true
}

async function submitCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await adminApi.createPromptVersion(code.value, { ...createForm.value })
    ElMessage.success('版本已创建')
    createVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function publish(version: string) {
  await ElMessageBox.confirm(`确认发布 Prompt ${code.value}@${version}？`, '发布确认', { type: 'warning' })
  try {
    await adminApi.publishPrompt(code.value, version)
    ElMessage.success('发布成功')
    load()
  } catch {
    /* handled by interceptor */
  }
}

async function rollback(version: string) {
  await ElMessageBox.confirm(`确认将 ${code.value} 回滚至 ${version}？`, '回滚确认', { type: 'warning' })
  try {
    await adminApi.rollbackPrompt(code.value, version)
    ElMessage.success('回滚成功')
    load()
  } catch {
    /* handled by interceptor */
  }
}

load()
</script>

<style scoped>
.page-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.page-filter-control {
  width: 220px;
}

.prompt-editor :deep(textarea) {
  font-family: ui-monospace, 'Cascadia Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
}
</style>
