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
            @change="onAppFilterChange"
            @clear="onAppFilterChange"
          />
          <el-select
            v-model="code"
            filterable
            clearable
            placeholder="选择 AI 作业"
            class="page-filter-control page-filter-control--wide"
            @change="load"
          >
            <el-option v-for="task in filteredTasks" :key="task.code" :label="`${task.code} · ${task.name}`" :value="task.code" />
          </el-select>
          <el-button type="primary" @click="openCreate">新建版本</el-button>
        </div>
      </div>
    </div>

    <div v-loading="loading" class="table-panel">
      <div class="table-panel-header">
        <div>
          <h3 class="table-panel-title">Prompt 版本</h3>
          <p class="table-panel-subtitle">作业 code: {{ code || '未选择' }} · 不可变版本，编辑将 fork 新版本</p>
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
              {{ row.status === 'PUBLISHED' ? '已发布' : row.status === 'DRAFT' ? '草稿' : row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="模板摘要" min-width="280">
          <template #default="{ row }">
            <div class="template-summary">
              <div v-for="(line, idx) in summaryLines(row.templateBody)" :key="idx" class="summary-line" :title="line">
                {{ line }}
              </div>
              <span v-if="!summaryLines(row.templateBody).length" class="summary-empty">—</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" width="170" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" class="action-btn" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" class="action-btn" @click="openFork(row)">编辑</el-button>
            <template v-if="row.status === 'PUBLISHED'">
              <el-tag size="small" type="success" effect="plain">当前</el-tag>
            </template>
            <template v-else>
              <el-button link type="success" class="action-btn" @click="publish(row.version)">发布</el-button>
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
            <el-button
              v-if="publishedVersion && publishedVersion !== row.version"
              link
              type="info"
              class="action-btn"
              @click="compareWithPublished(row.version)"
            >
              对比
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <PromptVersionDetailDialog ref="detailDialogRef" />
    <PromptVersionEditorDialog ref="editorDialogRef" @saved="load" />
    <VersionDiffDialog ref="diffDialogRef" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import VersionDiffDialog from '../components/VersionDiffDialog.vue'
import PromptVersionDetailDialog from '../components/PromptVersionDetailDialog.vue'
import PromptVersionEditorDialog from '../components/PromptVersionEditorDialog.vue'
import AppSelect from '../components/AppSelect.vue'
import { adminApi, normalizePage, type PromptVersionVO, type TaskVO } from '../api/admin'
import { filterTasksByApp, getLastAppKey, syncTaskCode } from '../utils/lastAppKey'
import { templateSummaryLines } from '../utils/promptVersion'

const tasks = ref<TaskVO[]>([])
const filterAppKey = ref(getLastAppKey())
const filteredTasks = computed(() => filterTasksByApp(tasks.value, filterAppKey.value))
const code = ref('aiChat')
const loading = ref(false)
const versions = ref<PromptVersionVO[]>([])
const detailDialogRef = ref<InstanceType<typeof PromptVersionDetailDialog> | null>(null)
const editorDialogRef = ref<InstanceType<typeof PromptVersionEditorDialog> | null>(null)
const diffDialogRef = ref<InstanceType<typeof VersionDiffDialog> | null>(null)
const publishedVersion = computed(() => versions.value.find((v) => v.status === 'PUBLISHED')?.version)
const versionCodes = computed(() => versions.value.map((v) => v.version))

function summaryLines(body?: string) {
  return templateSummaryLines(body, 5)
}

async function loadTasks() {
  const data = await adminApi.listTasks(1, 500)
  tasks.value = normalizePage(data, 1, 500).records
  code.value = syncTaskCode(filteredTasks.value, code.value)
  if (!code.value && filteredTasks.value.length) {
    code.value = filteredTasks.value[0].code
  }
}

function onAppFilterChange() {
  code.value = syncTaskCode(filteredTasks.value, code.value)
  load()
}

async function load() {
  if (!code.value) {
    versions.value = []
    return
  }
  loading.value = true
  try {
    versions.value = await adminApi.listPromptVersions(code.value)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  if (!code.value) {
    ElMessage.warning('请先选择 AI 作业')
    return
  }
  editorDialogRef.value?.openCreate({ taskCode: code.value, existingVersions: versionCodes.value })
}

function openDetail(row: PromptVersionVO) {
  if (!code.value) return
  detailDialogRef.value?.open({ taskCode: code.value, version: row })
}

function openFork(row: PromptVersionVO) {
  if (!code.value) return
  editorDialogRef.value?.openFork({
    taskCode: code.value,
    base: row,
    existingVersions: versionCodes.value
  })
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

function compareWithPublished(version: string) {
  const published = publishedVersion.value
  if (!published || !diffDialogRef.value) return
  diffDialogRef.value.open({
    title: `Prompt 对比 · ${published} → ${version}`,
    loader: () => adminApi.diffPrompt(code.value, published, version)
  })
}

onMounted(async () => {
  await loadTasks()
  await load()
})
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

.page-filter-control--wide {
  width: 280px;
}

.template-summary {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 2px 0;
}

.summary-line {
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-regular);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.summary-empty {
  color: var(--el-text-color-placeholder);
}
</style>
