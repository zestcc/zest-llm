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
            v-model="taskCode"
            filterable
            placeholder="选择 AI 作业"
            class="page-filter-control page-filter-control--wide"
            @change="loadRoute"
          >
            <el-option v-for="task in filteredTasks" :key="task.code" :label="`${task.code} · ${task.name}`" :value="task.code" />
          </el-select>
          <el-button type="primary" :loading="loading" @click="loadRoute">加载</el-button>
        </div>
      </div>
    </div>

    <div v-loading="loading" class="table-panel route-form-panel">
      <div class="table-panel-header">
        <div>
          <h3 class="table-panel-title">模型路由</h3>
          <p class="table-panel-subtitle">作业: {{ taskCode || '未选择' }}</p>
        </div>
        <el-button type="primary" :loading="saving" :disabled="!taskCode" @click="saveRoute">保存</el-button>
      </div>
      <el-form :model="form" label-width="120px" class="route-form">
        <el-form-item label="主模型">
          <el-input v-model="form.primaryModel" placeholder="如 gpt-4o-mini" />
        </el-form-item>
        <el-form-item label="Fallback 模型">
          <el-input v-model="form.fallbackModels" placeholder="逗号分隔，如 gpt-3.5-turbo" />
        </el-form-item>
        <el-form-item label="Max Tokens">
          <el-input-number v-model="form.maxTokens" :min="1" :max="128000" controls-position="right" style="width: 220px" />
        </el-form-item>
        <el-form-item label="Temperature">
          <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" :precision="2" controls-position="right" style="width: 220px" />
        </el-form-item>
        <el-form-item label="Timeout (ms)">
          <el-input-number v-model="form.timeoutMs" :min="1000" :step="1000" controls-position="right" style="width: 220px" />
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AppSelect from '../components/AppSelect.vue'
import { adminApi, normalizePage, type ModelRouteVO, type TaskVO } from '../api/admin'
import { filterTasksByApp, getLastAppKey, syncTaskCode } from '../utils/lastAppKey'

const loading = ref(false)
const saving = ref(false)
const tasks = ref<TaskVO[]>([])
const filterAppKey = ref(getLastAppKey())
const filteredTasks = computed(() => filterTasksByApp(tasks.value, filterAppKey.value))
const taskCode = ref('aiChat')

const form = reactive<ModelRouteVO>({
  primaryModel: '',
  fallbackModels: '',
  maxTokens: 1024,
  temperature: 0.7,
  timeoutMs: 30000
})

async function loadTasks() {
  const data = await adminApi.listTasks(1, 500)
  tasks.value = normalizePage(data, 1, 500).records
  taskCode.value = syncTaskCode(filteredTasks.value, taskCode.value)
  if (!taskCode.value && filteredTasks.value.length) {
    taskCode.value = filteredTasks.value[0].code
  }
}

async function onAppFilterChange() {
  taskCode.value = syncTaskCode(filteredTasks.value, taskCode.value)
  await loadRoute()
}

async function loadRoute() {
  if (!taskCode.value) return
  loading.value = true
  try {
    const data = await adminApi.getModelRoute(taskCode.value)
    form.primaryModel = data.primaryModel || ''
    form.fallbackModels = data.fallbackModels || ''
    form.maxTokens = data.maxTokens ?? 1024
    form.temperature = data.temperature ?? 0.7
    form.timeoutMs = data.timeoutMs ?? 30000
  } finally {
    loading.value = false
  }
}

async function saveRoute() {
  if (!taskCode.value) return
  if (!form.primaryModel) {
    ElMessage.warning('请填写主模型')
    return
  }
  saving.value = true
  try {
    await adminApi.updateModelRoute(taskCode.value, { ...form })
    ElMessage.success('模型路由已保存')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadTasks()
  await loadRoute()
})
</script>

<style scoped>
.page-filter-control--wide {
  width: 280px;
}

.route-form-panel {
  padding-bottom: 8px;
}

.route-form {
  padding: 8px 16px 20px;
  max-width: 640px;
}
</style>
