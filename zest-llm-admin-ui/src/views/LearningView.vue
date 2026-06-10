<template>
  <div class="learning-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>自我改进闭环</span>
          <el-select v-model="taskCode" filterable placeholder="选择作业" style="width: 220px" @change="loadAll">
            <el-option v-for="t in tasks" :key="t.code" :label="t.code" :value="t.code" />
          </el-select>
        </div>
      </template>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-button type="primary" :loading="suggestLoading" @click="loadSuggestions">建议 Eval 样本</el-button>
          <el-checkbox-group v-model="sources" style="margin-left: 12px" @change="loadSuggestions">
            <el-checkbox label="execution">Execution 失败</el-checkbox>
            <el-checkbox label="langfuse">Langfuse 低分</el-checkbox>
          </el-checkbox-group>
          <el-table v-loading="suggestLoading" :data="suggestions" stripe empty-text="暂无建议" style="margin-top: 12px">
            <el-table-column prop="traceId" label="traceId" min-width="160" />
            <el-table-column prop="source" label="来源" width="160" />
            <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
          </el-table>
        </el-col>
        <el-col :span="12">
          <el-form inline>
            <el-form-item label="Profile 版本">
              <el-input v-model="profileVersion" placeholder="v2" style="width: 120px" />
            </el-form-item>
            <el-form-item>
              <el-button type="success" :loading="cycleLoading" @click="runCycle(true)">试运行闭环</el-button>
            </el-form-item>
          </el-form>
          <el-alert v-if="cycleResult" :title="cycleResult.message || cycleResult.status" type="info" show-icon />
          <el-table v-loading="cycleListLoading" :data="cycles" stripe empty-text="暂无历史" style="margin-top: 12px">
            <el-table-column prop="runCode" label="runCode" width="120" />
            <el-table-column prop="profileVersion" label="版本" width="80" />
            <el-table-column prop="passRate" label="通过率" width="90" />
            <el-table-column prop="status" label="状态" width="90" />
            <el-table-column prop="message" label="说明" min-width="160" show-overflow-tooltip />
          </el-table>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi, normalizePage, type EvalCaseSuggestion, type LearningCycleResult, type LearningCycleRunVO, type TaskVO } from '../api/admin'

const tasks = ref<TaskVO[]>([])
const taskCode = ref('aiChat')
const profileVersion = ref('v1')
const suggestions = ref<EvalCaseSuggestion[]>([])
const cycles = ref<LearningCycleRunVO[]>([])
const cycleResult = ref<LearningCycleResult | null>(null)
const suggestLoading = ref(false)
const cycleLoading = ref(false)
const cycleListLoading = ref(false)
const sources = ref(['execution', 'langfuse'])

async function loadTasks() {
  const data = await adminApi.listTasks(1, 200)
  tasks.value = normalizePage(data, 1, 200).records
  if (tasks.value.length && !taskCode.value) {
    taskCode.value = tasks.value[0].code
  }
}

async function loadSuggestions() {
  if (!taskCode.value) return
  suggestLoading.value = true
  try {
    suggestions.value = await adminApi.suggestLearningCases({
      taskCode: taskCode.value,
      limit: 20,
      distillationSources: sources.value
    })
  } finally {
    suggestLoading.value = false
  }
}

async function runCycle(dryRun: boolean) {
  if (!taskCode.value || !profileVersion.value) return
  cycleLoading.value = true
  try {
    cycleResult.value = await adminApi.runLearningCycle({
      taskCode: taskCode.value,
      profileVersion: profileVersion.value,
      dryRun
    })
    ElMessage.success('闭环任务已完成')
    await loadCycles()
  } finally {
    cycleLoading.value = false
  }
}

async function loadCycles() {
  if (!taskCode.value) return
  cycleListLoading.value = true
  try {
    const page = await adminApi.listLearningCycles(taskCode.value, 1, 20)
    cycles.value = page.records || []
  } finally {
    cycleListLoading.value = false
  }
}

async function loadAll() {
  await Promise.all([loadSuggestions(), loadCycles()])
}

onMounted(async () => {
  await loadTasks()
  await loadAll()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
