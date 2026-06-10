<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="820px"
    destroy-on-close
    @closed="onClosed"
  >
    <div v-loading="loading" class="diff-shell">
      <div v-if="diff" class="diff-meta">
        <el-tag effect="plain">{{ diff.fromVersion }}</el-tag>
        <el-icon><Right /></el-icon>
        <el-tag type="success" effect="plain">{{ diff.toVersion }}</el-tag>
      </div>

      <el-empty v-if="!loading && (!diff?.changes?.length)" description="两版本无差异" />

      <el-table v-else :data="diff?.changes || []" stripe size="small" class="diff-table">
        <el-table-column prop="field" label="字段" width="160" />
        <el-table-column prop="changeType" label="变更" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.changeType === 'ADDED' ? 'success' : row.changeType === 'REMOVED' ? 'danger' : 'warning'"
              size="small"
            >
              {{ changeLabel(row.changeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变更前" min-width="240">
          <template #default="{ row }">
            <pre v-if="row.unifiedDiff" class="diff-text diff-unified">{{ row.unifiedDiff }}</pre>
            <pre v-else-if="row.before" class="diff-text">{{ row.before }}</pre>
            <span v-else class="diff-empty">—</span>
          </template>
        </el-table-column>
        <el-table-column label="变更后" min-width="240">
          <template #default="{ row }">
            <pre v-if="!row.unifiedDiff && row.after" class="diff-text">{{ row.after }}</pre>
            <span v-else-if="row.unifiedDiff" class="diff-empty">见左侧 unified diff</span>
            <span v-else class="diff-empty">—</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Right } from '@element-plus/icons-vue'
import type { VersionDiffVO } from '../api/admin'

const visible = ref(false)
const loading = ref(false)
const title = ref('版本对比')
const diff = ref<VersionDiffVO | null>(null)

function changeLabel(type?: string) {
  if (type === 'ADDED') return '新增'
  if (type === 'REMOVED') return '删除'
  if (type === 'MODIFIED') return '修改'
  return type || '—'
}

async function open(opts: { title: string; loader: () => Promise<VersionDiffVO> }) {
  title.value = opts.title
  visible.value = true
  loading.value = true
  diff.value = null
  try {
    diff.value = await opts.loader()
  } finally {
    loading.value = false
  }
}

function onClosed() {
  diff.value = null
}

defineExpose({ open })
</script>

<style scoped>
.diff-shell {
  min-height: 120px;
}

.diff-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.diff-table :deep(.cell) {
  vertical-align: top;
}

.diff-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: ui-monospace, 'Cascadia Code', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.5;
  max-height: 200px;
  overflow: auto;
}

.diff-unified {
  max-height: 320px;
}

.diff-empty {
  color: var(--el-text-color-placeholder);
}
</style>
