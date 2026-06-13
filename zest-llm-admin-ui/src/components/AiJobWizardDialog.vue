<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="520px"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form v-loading="templatesLoading" :model="form" label-width="100px">
      <el-form-item v-if="showTemplateSelect" label="场景模板" required>
        <el-select v-model="form.templateId" placeholder="选择模板" style="width: 100%" @change="onTemplateChange">
          <el-option
            v-for="tpl in templates"
            :key="tpl.id"
            :label="tpl.name"
            :value="tpl.id || ''"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-else label="模板">
        <el-input :model-value="form.templateName" disabled />
      </el-form-item>
      <el-form-item label="所属应用" required>
        <el-input v-model="form.appKey" placeholder="order-service" />
      </el-form-item>
      <el-form-item label="作业 Code" required>
        <el-input v-model="form.taskCode" />
      </el-form-item>
      <el-form-item label="立即发布">
        <el-switch v-model="form.publish" />
      </el-form-item>
      <el-form-item label="运行 Probe">
        <el-switch v-model="form.runProbe" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, type AiJobWizardResult, type ScenarioTemplateVO } from '../api/admin'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title?: string
    /** 预选模板 ID；为空时在对话框内选择 */
    initialTemplateId?: string
    showTemplateSelect?: boolean
  }>(),
  {
    title: '从场景模板创建作业',
    showTemplateSelect: true
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [result: AiJobWizardResult]
}>()

const router = useRouter()
const templatesLoading = ref(false)
const submitting = ref(false)
const templates = ref<ScenarioTemplateVO[]>([])

const form = reactive({
  templateId: '',
  templateName: '',
  appKey: 'order-service',
  taskCode: '',
  publish: false,
  runProbe: true
})

function findTemplate(id: string) {
  return templates.value.find((t) => t.id === id)
}

function applyTemplate(tpl: ScenarioTemplateVO | undefined) {
  if (!tpl) return
  form.templateId = tpl.id || ''
  form.templateName = tpl.name || ''
  form.taskCode = tpl.taskCodeSuggestion || ''
}

function onTemplateChange(id: string) {
  applyTemplate(findTemplate(id))
}

async function loadTemplates() {
  templatesLoading.value = true
  try {
    const res = await adminApi.listScenarioTemplates()
    templates.value = res.data ?? res ?? []
    if (props.initialTemplateId) {
      applyTemplate(findTemplate(props.initialTemplateId))
    } else if (props.showTemplateSelect && templates.value.length > 0 && !form.templateId) {
      applyTemplate(templates.value[0])
    }
  } finally {
    templatesLoading.value = false
  }
}

async function submit() {
  if (!form.templateId || !form.appKey.trim() || !form.taskCode.trim()) {
    ElMessage.warning('请填写模板、应用与作业 Code')
    return
  }
  submitting.value = true
  try {
    const res = await adminApi.runAiJobWizard({
      templateId: form.templateId,
      appKey: form.appKey.trim(),
      taskCode: form.taskCode.trim(),
      publish: form.publish,
      runProbe: form.runProbe
    })
    const data = (res.data ?? res) as AiJobWizardResult
    ElMessage.success(`向导完成：${data.taskCode} / ${data.profileVersion} Probe=${data.probeStatus}`)
    emit('update:modelValue', false)
    emit('success', data)
    if (data.nextUrl) {
      try {
        await ElMessageBox.confirm('是否前往智能体配置继续编辑？', '向导完成', {
          confirmButtonText: '前往配置',
          cancelButtonText: '留在此页',
          type: 'success'
        })
        await router.push(data.nextUrl)
      } catch {
        /* 用户取消 */
      }
    }
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    form.appKey = 'order-service'
    form.publish = false
    form.runProbe = true
    if (props.initialTemplateId) {
      form.templateId = props.initialTemplateId
      form.templateName = ''
    } else {
      form.templateId = ''
      form.templateName = ''
      form.taskCode = ''
    }
    loadTemplates()
  }
)
</script>
