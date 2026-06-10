<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row">
        <div class="page-filters">
          <el-button type="primary" :loading="loading" @click="loadDatasets">刷新数据集</el-button>
          <el-button @click="createDemoDataset">新建数据集</el-button>
        </div>
      </div>
    </div>

    <div v-loading="loading" class="table-panel">
      <div class="table-panel-header">
        <h3 class="table-panel-title">Eval 数据集</h3>
      </div>
      <el-table :data="datasets" stripe empty-text="暂无数据集">
        <el-table-column prop="datasetCode" label="Code" width="160" />
        <el-table-column prop="datasetName" label="名称" min-width="180" />
        <el-table-column prop="appKey" label="App" width="140" />
        <el-table-column prop="taskCode" label="Task" width="120" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :loading="runningCode === row.datasetCode" @click="runEval(row.datasetCode)">
              运行 Eval
            </el-button>
            <el-button link type="info" @click="loadRuns(row.datasetCode)">历史</el-button>
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
        :status="lastRun.status === 'COMPLETED' ? 'success' : 'warning'"
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
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi, type EvalDatasetVO, type EvalRunVO } from '../api/admin'

const loading = ref(false)
const runningCode = ref('')
const datasets = ref<EvalDatasetVO[]>([])
const runs = ref<EvalRunVO[]>([])
const lastRun = ref<EvalRunVO | null>(null)
const selectedDataset = ref('')

const failedCases = computed(() => (lastRun.value?.caseResults || []).filter((c) => !c.passed))

async function loadDatasets() {
  loading.value = true
  try {
    datasets.value = await adminApi.listEvalDatasets()
  } finally {
    loading.value = false
  }
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
  selectedDataset.value = datasetCode
  runs.value = await adminApi.listEvalRuns(datasetCode)
}

async function createDemoDataset() {
  const code = `eval-${Date.now().toString().slice(-6)}`
  await adminApi.createEvalDataset({
    datasetCode: code,
    datasetName: `Quick ${code}`,
    appKey: 'order-service',
    taskCode: 'aiChat'
  })
  ElMessage.success(`已创建数据集 ${code}`)
  await loadDatasets()
}

loadDatasets()
</script>
