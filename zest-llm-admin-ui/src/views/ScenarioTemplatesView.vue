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

    <AiJobWizardDialog
      v-model="applyVisible"
      title="应用场景模板"
      :initial-template-id="selectedTemplateId"
      :show-template-select="false"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { adminApi, type ScenarioTemplateVO } from '../api/admin'
import AiJobWizardDialog from '../components/AiJobWizardDialog.vue'

const loading = ref(false)
const templates = ref<ScenarioTemplateVO[]>([])
const applyVisible = ref(false)
const selectedTemplateId = ref('')

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
  selectedTemplateId.value = tpl.id || ''
  applyVisible.value = true
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
