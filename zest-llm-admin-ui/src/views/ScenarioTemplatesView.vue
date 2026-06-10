<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row">
        <p class="page-desc">
          B 层场景模板：一键生成 Task + Profile 草稿（对话 / 报表 / 运维）
        </p>
        <el-button type="primary" :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-row v-loading="loading" :gutter="16">
      <el-col v-for="tpl in templates" :key="tpl.id" :xs="24" :md="8">
        <div class="table-panel template-card">
          <h3>{{ tpl.name }}</h3>
          <p class="tpl-desc">{{ tpl.description }}</p>
          <div class="tpl-meta">
            <el-tag size="small">Tier: {{ tpl.recommendedTier }}</el-tag>
            <el-tag v-if="tpl.requiresMcp" size="small" type="warning">MCP</el-tag>
            <el-tag v-if="tpl.requiresKnowledge" size="small" type="success">RAG</el-tag>
          </div>
          <p class="tpl-code">建议 code: {{ tpl.taskCodeSuggestion }}</p>
          <el-button type="primary" @click="openApply(tpl)">应用模板</el-button>
        </div>
      </el-col>
    </el-row>

    <el-dialog v-model="applyVisible" title="应用场景模板" width="480px" destroy-on-close>
      <el-form :model="applyForm" label-width="100px">
        <el-form-item label="模板">
          <el-input :model-value="applyForm.templateName" disabled />
        </el-form-item>
        <el-form-item label="所属应用" required>
          <el-input v-model="applyForm.appKey" placeholder="order-service" />
        </el-form-item>
        <el-form-item label="作业 Code" required>
          <el-input v-model="applyForm.taskCode" />
        </el-form-item>
        <el-form-item label="立即发布">
          <el-switch v-model="applyForm.publish" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitApply">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { adminApi, type ScenarioTemplateVO } from '../api/admin'

const loading = ref(false)
const submitting = ref(false)
const templates = ref<ScenarioTemplateVO[]>([])
const applyVisible = ref(false)
const applyForm = ref({
  templateId: '',
  templateName: '',
  appKey: 'order-service',
  taskCode: '',
  publish: false
})

async function load() {
  loading.value = true
  try {
    const res = await adminApi.listScenarioTemplates()
    templates.value = res.data ?? res ?? []
  } finally {
    loading.value = false
  }
}

function openApply(tpl: ScenarioTemplateVO) {
  applyForm.value = {
    templateId: tpl.id || '',
    templateName: tpl.name || '',
    appKey: 'order-service',
    taskCode: tpl.taskCodeSuggestion || '',
    publish: false
  }
  applyVisible.value = true
}

async function submitApply() {
  submitting.value = true
  try {
    const res = await adminApi.applyScenarioTemplate({
      templateId: applyForm.value.templateId,
      appKey: applyForm.value.appKey,
      taskCode: applyForm.value.taskCode,
      publish: applyForm.value.publish
    })
    const data = res.data ?? res
    ElMessage.success(`已创建 Profile ${data.profileVersion}（${data.taskCode}）`)
    applyVisible.value = false
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.template-card {
  margin-bottom: 16px;
  height: calc(100% - 16px);
}
.tpl-desc {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  min-height: 48px;
}
.tpl-meta {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin: 10px 0;
}
.tpl-code {
  font-size: 12px;
  margin-bottom: 12px;
}
</style>
