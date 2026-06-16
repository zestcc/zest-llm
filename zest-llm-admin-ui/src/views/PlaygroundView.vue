<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row">
        <div class="page-filters">
          <AppSelect v-model="form.appKey" select-class="page-filter-control" width="160px" />
          <el-input v-model="form.code" placeholder="作业 code" class="page-filter-control" />
          <el-select v-model="selectedTemplate" placeholder="输入模板" clearable class="page-filter-control" @change="applyTemplate">
            <el-option v-for="t in templates" :key="t.name" :label="t.name" :value="t.name" />
          </el-select>
          <el-switch v-model="streamMode" active-text="流式" inactive-text="同步" inline-prompt />
          <el-button @click="saveTemplate">保存为模板</el-button>
          <el-button type="primary" @click="preview">预览 Prompt</el-button>
          <el-button type="success" :loading="running && !streaming" @click="runInvoke">运行 Invoke</el-button>
          <el-button v-if="streaming" type="danger" plain @click="stopStream">停止</el-button>
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
              <span v-if="streamStats.firstTokenMs != null"> · 首 token {{ streamStats.firstTokenMs }}ms</span>
              <span v-if="streamStats.latencyMs != null"> · 总耗时 {{ streamStats.latencyMs }}ms</span>
            </p>
          </div>
          <div ref="outputScrollRef" class="playground-output">
            <pre v-if="streamMode && streaming" class="playground-stream-text">{{ streamText }}<span class="playground-cursor">▍</span></pre>
            <el-input v-else v-model="outputText" type="textarea" :rows="14" class="prompt-editor" readonly />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, type PlaygroundRunVO } from '../api/admin'
import AppSelect from '../components/AppSelect.vue'
import { getLastAppKey } from '../utils/lastAppKey'

const TEMPLATE_KEY = 'zest-llm-playground-templates'

interface PlaygroundTemplate {
  name: string
  appKey: string
  code: string
  inputsJson: string
}

interface StreamMeta {
  traceId?: string
  code?: string
  promptVersion?: string
  model?: string
}

interface StreamDonePayload extends PlaygroundRunVO {
  metrics?: {
    latencyMs?: number
    promptTokens?: number
    completionTokens?: number
    cacheHit?: boolean
  }
}

interface StreamErrorPayload {
  traceId?: string
  status?: string
  errorCode?: string
  errorMessage?: string
}

const form = ref({ appKey: getLastAppKey(), code: 'aiChat' })
const inputsJson = ref('{\n  "question": "hello playground"\n}')
const outputText = ref('')
const streamText = ref('')
const previewResult = ref<{ model?: string; promptVersion?: string } | null>(null)
const running = ref(false)
const streaming = ref(false)
const streamMode = ref(true)
const templates = ref<PlaygroundTemplate[]>([])
const selectedTemplate = ref('')
const outputScrollRef = ref<HTMLElement | null>(null)
const streamStats = ref<{ firstTokenMs?: number; latencyMs?: number }>({})

let abortController: AbortController | null = null
let streamStartedAt = 0
let firstTokenAt: number | null = null

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

async function scrollOutputToBottom() {
  await nextTick()
  const el = outputScrollRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

function formatRunResult(data: PlaygroundRunVO): string {
  return JSON.stringify(
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
}

function resetStreamState() {
  streamText.value = ''
  streamStats.value = {}
  firstTokenAt = null
}

function stopStream() {
  abortController?.abort()
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

async function runInvokeStream() {
  resetStreamState()
  streaming.value = true
  running.value = true
  streamStartedAt = Date.now()
  abortController = new AbortController()

  const inputs = parseInputs()
  const body = {
    appKey: form.value.appKey,
    code: form.value.code,
    inputs,
    bizId: 'playground'
  }

  try {
    await adminApi.playgroundRunStream(body, (event, raw) => {
      if (event === 'meta') {
        const meta = JSON.parse(raw) as StreamMeta
        previewResult.value = {
          model: meta.model,
          promptVersion: meta.promptVersion
        }
        return
      }
      if (event === 'delta') {
        if (firstTokenAt == null) {
          firstTokenAt = Date.now()
          streamStats.value = {
            ...streamStats.value,
            firstTokenMs: firstTokenAt - streamStartedAt
          }
        }
        streamText.value += raw
        void scrollOutputToBottom()
        return
      }
      if (event === 'done') {
        const done = JSON.parse(raw) as StreamDonePayload
        previewResult.value = {
          model: done.model ?? previewResult.value?.model,
          promptVersion: done.promptVersion ?? previewResult.value?.promptVersion
        }
        streamStats.value = {
          firstTokenMs: streamStats.value.firstTokenMs,
          latencyMs: done.metrics?.latencyMs ?? Date.now() - streamStartedAt
        }
        outputText.value = formatRunResult(done)
        ElMessage.success('流式 Invoke 完成')
        return
      }
      if (event === 'error') {
        const err = JSON.parse(raw) as StreamErrorPayload
        outputText.value = formatRunResult({
          traceId: err.traceId,
          status: err.status,
          errorCode: err.errorCode,
          errorMessage: err.errorMessage
        })
        ElMessage.warning(err.errorMessage || 'Invoke 失败')
      }
    }, abortController.signal)
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') {
      ElMessage.info('已停止生成')
      if (streamText.value) {
        outputText.value = streamText.value
      }
    } else {
      const message = err instanceof Error ? err.message : '流式请求失败'
      ElMessage.error(message)
    }
  } finally {
    streaming.value = false
    running.value = false
    abortController = null
  }
}

async function runInvokeSync() {
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
    outputText.value = formatRunResult(data)
    ElMessage[data.status === 'SUCCESS' ? 'success' : 'warning'](
      data.status === 'SUCCESS' ? 'Invoke 成功' : 'Invoke 失败'
    )
  } finally {
    running.value = false
  }
}

async function runInvoke() {
  if (streamMode.value) {
    await runInvokeStream()
  } else {
    await runInvokeSync()
  }
}

onMounted(loadTemplates)
</script>

<style scoped>
.playground-output {
  min-height: 320px;
  max-height: 320px;
  overflow: auto;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  background: var(--el-fill-color-blank);
}

.playground-stream-text {
  margin: 0;
  padding: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--el-font-family);
  font-size: 14px;
  line-height: 1.6;
}

.playground-cursor {
  animation: playground-blink 1s step-end infinite;
  color: var(--el-color-primary);
}

@keyframes playground-blink {
  50% {
    opacity: 0;
  }
}
</style>
