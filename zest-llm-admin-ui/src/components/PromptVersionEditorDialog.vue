<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="760px"
    destroy-on-close
    class="prompt-editor-dialog"
    @closed="onClosed"
  >
    <el-alert
      v-if="mode === 'fork' && baseVersion"
      type="info"
      :closable="false"
      show-icon
      class="fork-hint"
    >
      基于 <strong>{{ baseVersion }}</strong> 编辑，将创建新版本
      <strong>{{ form.version || '（自动）' }}</strong>
      {{ publishOnSave ? '并发布为当前线上版本' : '为草稿' }}（历史版本保留不变）。
    </el-alert>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="版本号" prop="version">
        <el-input v-model="form.version" placeholder="如 v3" style="max-width: 200px" />
        <span v-if="mode === 'fork'" class="version-hint">留空则自动递增</span>
      </el-form-item>
      <el-form-item label="模板内容" prop="templateBody">
        <el-input
          v-model="form.templateBody"
          type="textarea"
          :rows="16"
          class="prompt-editor"
          placeholder="Handlebars 模板；每条规则或要点单独一行"
        />
      </el-form-item>
      <el-form-item label="输出 Schema">
        <el-input
          v-model="form.outputSchema"
          type="textarea"
          :rows="5"
          class="prompt-editor"
          placeholder='{"type":"object","properties":{...}}'
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button v-if="mode === 'create'" :loading="saving" @click="submit(false)">保存草稿</el-button>
      <el-button v-if="mode === 'fork'" :loading="saving" @click="submit(false)">仅保存草稿</el-button>
      <el-button type="primary" :loading="saving" @click="submit(mode === 'fork' || publishOnSave)">
        {{ mode === 'fork' ? '保存并发布' : '创建草稿' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { adminApi, type PromptVersionVO } from '../api/admin'
import { suggestNextPromptVersion } from '../utils/promptVersion'

type EditorMode = 'create' | 'fork'

const emit = defineEmits<{ saved: [] }>()

const visible = ref(false)
const saving = ref(false)
const mode = ref<EditorMode>('create')
const title = ref('')
const taskCode = ref('')
const baseVersion = ref('')
const publishOnSave = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  version: '',
  templateBody: '',
  outputSchema: ''
})

const rules: FormRules = {
  templateBody: [{ required: true, message: '请输入模板内容', trigger: 'blur' }]
}

function openCreate(opts: { taskCode: string; existingVersions: string[] }) {
  mode.value = 'create'
  taskCode.value = opts.taskCode
  baseVersion.value = ''
  publishOnSave.value = false
  title.value = '新建 Prompt 版本'
  form.version = suggestNextPromptVersion(opts.existingVersions)
  form.templateBody = ''
  form.outputSchema = ''
  visible.value = true
}

function openFork(opts: { taskCode: string; base: PromptVersionVO; existingVersions: string[] }) {
  mode.value = 'fork'
  taskCode.value = opts.taskCode
  baseVersion.value = opts.base.version
  publishOnSave.value = true
  title.value = `编辑 Prompt · 基于 ${opts.base.version}`
  form.version = suggestNextPromptVersion(opts.existingVersions)
  form.templateBody = opts.base.templateBody || ''
  form.outputSchema = opts.base.outputSchema || ''
  visible.value = true
}

async function submit(publish: boolean) {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (mode.value === 'fork') {
      await adminApi.forkPromptVersion(taskCode.value, {
        baseVersion: baseVersion.value,
        version: form.version.trim() || undefined,
        templateBody: form.templateBody,
        outputSchema: form.outputSchema || undefined,
        publish
      })
      ElMessage.success(publish ? '新版本已创建并发布' : '新版本草稿已保存')
    } else {
      await adminApi.createPromptVersion(taskCode.value, {
        version: form.version,
        templateBody: form.templateBody,
        outputSchema: form.outputSchema || undefined
      })
      ElMessage.success('版本已创建')
    }
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

function onClosed() {
  formRef.value?.resetFields()
}

defineExpose({ openCreate, openFork })
</script>

<style scoped>
.fork-hint {
  margin-bottom: 16px;
}

.version-hint {
  margin-left: 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.prompt-editor :deep(textarea) {
  font-family: ui-monospace, 'Cascadia Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.65;
}
</style>
