<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row">
        <div class="page-filters">
          <el-input v-model="form.appKey" placeholder="appKey" class="page-filter-control" />
          <el-input v-model="form.code" placeholder="作业 code" class="page-filter-control" />
          <el-button type="primary" @click="preview">预览 Prompt</el-button>
          <el-button type="success" :loading="running" @click="runInvoke">运行 Invoke</el-button>
        </div>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="12">
        <div class="table-panel">
          <div class="table-panel-header">
            <h3 class="table-panel-title">输入 (JSON)</h3>
          </div>
          <el-input v-model="inputsJson" type="textarea" :rows="14" class="prompt-editor" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="table-panel">
          <div class="table-panel-header">
            <h3 class="table-panel-title">渲染结果 / 输出</h3>
            <p v-if="previewResult" class="table-panel-subtitle">
              model={{ previewResult.model }} · version={{ previewResult.promptVersion }}
            </p>
          </div>
          <el-input v-model="outputText" type="textarea" :rows="14" class="prompt-editor" readonly />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../api/admin'

const form = ref({ appKey: 'order-service', code: 'aiChat' })
const inputsJson = ref('{\n  "question": "hello playground"\n}')
const outputText = ref('')
const previewResult = ref<{ model?: string; promptVersion?: string } | null>(null)
const running = ref(false)

function parseInputs(): Record<string, unknown> {
  try {
    return JSON.parse(inputsJson.value || '{}')
  } catch {
    ElMessage.error('inputs JSON 格式无效')
    throw new Error('invalid json')
  }
}

async function preview() {
  try {
    const inputs = parseInputs()
    const data = await adminApi.playgroundPreview({
      appKey: form.value.appKey,
      code: form.value.code,
      inputs
    })
    previewResult.value = data
    outputText.value = data.renderedPrompt || ''
    ElMessage.success('Prompt 预览完成')
  } catch {
    /* handled */
  }
}

async function runInvoke() {
  running.value = true
  try {
    const inputs = parseInputs()
    const data = await adminApi.playgroundRun({
      appKey: form.value.appKey,
      code: form.value.code,
      inputs,
      bizId: 'playground'
    })
    previewResult.value = data
    outputText.value = JSON.stringify(
      {
        traceId: data.traceId,
        status: data.status,
        cacheHit: data.cacheHit,
        output: data.output,
        metrics: data.metrics,
        errorCode: data.errorCode,
        errorMessage: data.errorMessage
      },
      null,
      2
    )
    ElMessage[data.status === 'SUCCESS' ? 'success' : 'warning'](
      data.status === 'SUCCESS' ? 'Invoke 成功' : 'Invoke 失败'
    )
  } finally {
    running.value = false
  }
}
</script>
