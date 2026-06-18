<template>
  <el-drawer v-model="visible" :title="title" size="640px" destroy-on-close @closed="onClosed">
    <div v-loading="loading" class="detail-shell">
      <template v-if="version">
        <el-descriptions :column="2" border size="small" class="detail-meta">
          <el-descriptions-item label="版本">{{ version.version }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag
              :type="version.status === 'PUBLISHED' ? 'success' : version.status === 'DRAFT' ? 'warning' : 'info'"
              size="small"
            >
              {{ version.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ version.publishedAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ version.createdAt || '—' }}</el-descriptions-item>
          <el-descriptions-item v-if="version.createdBy" label="发布人" :span="2">
            {{ version.createdBy }}
          </el-descriptions-item>
        </el-descriptions>

        <section class="detail-section">
          <h4 class="detail-section-title">模板内容</h4>
          <pre class="detail-pre">{{ version.templateBody || '' }}</pre>
        </section>

        <section v-if="version.outputSchema" class="detail-section">
          <h4 class="detail-section-title">输出 Schema</h4>
          <pre class="detail-pre detail-pre--schema">{{ formattedSchema }}</pre>
        </section>
      </template>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { PromptVersionVO } from '../api/admin'

const visible = ref(false)
const loading = ref(false)
const title = ref('Prompt 详情')
const version = ref<PromptVersionVO | null>(null)

const formattedSchema = computed(() => {
  const raw = version.value?.outputSchema
  if (!raw) return ''
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
})

function open(opts: { taskCode: string; version: PromptVersionVO }) {
  title.value = `Prompt 详情 · ${opts.taskCode}@${opts.version.version}`
  version.value = opts.version
  visible.value = true
}

function onClosed() {
  version.value = null
}

defineExpose({ open })
</script>

<style scoped>
.detail-shell {
  min-height: 200px;
}

.detail-meta {
  margin-bottom: 20px;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.detail-pre {
  margin: 0;
  padding: 12px 14px;
  border-radius: var(--radius-md, 8px);
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
  white-space: pre-wrap;
  word-break: break-word;
  font-family: ui-monospace, 'Cascadia Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.65;
  max-height: 480px;
  overflow: auto;
}

.detail-pre--schema {
  max-height: 240px;
}
</style>
