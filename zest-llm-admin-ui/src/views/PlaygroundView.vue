<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row">
        <div class="page-filters">
          <el-input v-model="form.appKey" placeholder="appKey" class="page-filter-control" />
          <el-input v-model="form.code" placeholder="作业 code" class="page-filter-control" />
          <el-select v-model="selectedTemplate" placeholder="输入模板" clearable class="page-filter-control" @change="applyTemplate">
            <el-option v-for="t in templates" :key="t.name" :label="t.name" :value="t.name" />
          </el-select>
          <el-button @click="saveTemplate">保存为模板</el-button>
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
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../api/admin'

const TEMPLATE_KEY = 'zest-llm-playground-templates'

interface PlaygroundTemplate {
  name: string
  appKey: string
  code: string
  inputsJson: string
}

const form = ref({ appKey: 'order-service', code: 'aiChat' })
const inputsJson = ref('{\n  "question": "hello playground"\n}')
const outputText = ref('')
const previewResult = ref<{ model?: string; promptVersion?: string } | null>(null)
const running = ref(false)
const templates = ref<PlaygroundTemplate[]>([])
const selectedTemplate = ref('')

function loadTemplates() {
  try {
    templates.value = JSON.parse(localStorage.getItem(TEMPLATE_KEY) || '[]')
  } catch {
    templates.value = []
  }
}

function applyTemplate(name: string) {
  const tpl = templates.value.find((t) => t.name === name)
  if (!tpl) return
  form.value = { appKey: tpl.appKey, code: tpl.code }
  inputsJson.value = tpl.inputsJson
}

async function saveTemplate() {
  const { value } = await ElMessageBox.prompt('模板名称', '保存 Playground 模板', {
    inputValue: `${form.value.code}-inputs`
  })
  if (!value) return
  const next = templates.value.filter((t) => t.name !== value)
  next.unshift({
    name: value,
    appKey: form.value.appKey,
    code: form.value.code,
    inputsJson: inputsJson.value
  })
  templates.value = next.slice(0, 20)
  localStorage.setItem(TEMPLATE_KEY, JSON.stringify(templates.value))
  selectedTemplate.value = value
  ElMessage.success('模板已保存')
}

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

onMounted(loadTemplates)
</script>
