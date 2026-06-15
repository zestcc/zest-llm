<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-header-row">
        <el-button link @click="router.push('/plugin-catalog')">← 返回插件中心</el-button>
        <div class="header-actions">
          <el-button :loading="healthLoading" @click="runHealthCheck">探测健康</el-button>
          <el-button
            v-if="detail?.installed && !detail?.active"
            type="primary"
            :loading="setDefaultLoading"
            @click="setAsDefault"
          >
            设为默认
          </el-button>
        </div>
      </div>
    </div>

    <div v-loading="loading">
      <div v-if="detail" class="table-panel detail-header">
        <div class="detail-title-row">
          <div>
            <h2>{{ detail.pluginName }}</h2>
            <p v-if="detail.tagline" class="detail-tagline">{{ detail.tagline }}</p>
            <p class="detail-sub">
              {{ detail.spiType }} · {{ detail.pluginId }} · {{ detail.vendor }} v{{ detail.version }}
            </p>
          </div>
          <div class="detail-tags">
            <el-tag v-if="detail.recommendedTier" type="info">{{ tierLabel(detail.recommendedTier) }}</el-tag>
            <el-tag v-if="detail.active" type="success">当前激活</el-tag>
            <el-tag v-else-if="detail.loadStatus === 'NOT_INSTALLED'" type="info">未安装</el-tag>
            <el-tag v-else type="warning">未激活</el-tag>
            <el-tag :type="detail.healthUp ? 'success' : 'danger'">
              {{ detail.healthUp ? '健康' : '异常' }}
            </el-tag>
          </div>
        </div>
        <p class="detail-desc">{{ detail.description }}</p>
        <el-alert
          v-if="detail.restartRequired"
          type="warning"
          show-icon
          :closable="false"
          title="控制台已记录默认偏好，请同步 application.yml 并重启 Admin 后生效"
          class="restart-alert"
        />
        <div class="detail-kv">
          <span>配置项：<code>{{ detail.configProperty }}</code></span>
          <span>当前值：<code>{{ detail.configuredValue }}</code></span>
          <span v-if="detail.pendingValue">待生效：<code>{{ detail.pendingValue }}</code></span>
        </div>
      </div>

      <el-row v-if="detail" :gutter="16">
        <el-col :xs="24" :lg="14">
          <div v-if="detail.overview" class="table-panel">
            <h3 class="table-panel-title">概述</h3>
            <p v-for="(para, idx) in overviewParagraphs" :key="idx" class="overview-para">{{ para }}</p>
          </div>

          <div v-if="detail.architectureFlow" class="table-panel">
            <h3 class="table-panel-title">架构与数据流</h3>
            <div class="code-block architecture-block">
              <pre>{{ detail.architectureFlow.trim() }}</pre>
            </div>
          </div>

          <div class="table-panel">
            <h3 class="table-panel-title">分步集成指南</h3>
            <p class="table-panel-subtitle">按顺序完成；每步含验收标准，避免「配了但不知道是否生效」</p>
            <el-timeline>
              <el-timeline-item
                v-for="step in detail.integrationSteps || []"
                :key="step.stepId"
                :type="step.required ? 'primary' : 'info'"
              >
                <div class="step-card">
                  <div class="step-head">
                    <strong>{{ step.order }}. {{ step.title }}</strong>
                    <el-tag v-if="step.required" size="small" type="danger">必做</el-tag>
                    <el-tag v-else size="small" type="info">可选</el-tag>
                  </div>
                  <p>{{ step.description }}</p>
                  <ul v-if="step.hints?.length" class="step-hints">
                    <li v-for="(hint, hi) in step.hints" :key="hi">{{ hint }}</li>
                  </ul>
                  <p v-if="step.verificationCriteria" class="step-verify">
                    <strong>验收：</strong>{{ step.verificationCriteria }}
                  </p>
                  <div v-if="step.commandExample" class="code-block">
                    <pre>{{ step.commandExample }}</pre>
                  </div>
                  <div class="step-actions">
                    <el-button
                      v-if="step.actionType === 'NAVIGATE' && step.actionTarget"
                      size="small"
                      type="primary"
                      @click="go(step.actionTarget)"
                    >
                      {{ step.actionLabel || '前往' }}
                    </el-button>
                    <el-button
                      v-else-if="step.actionType === 'VERIFY'"
                      size="small"
                      @click="runHealthCheck"
                    >
                      {{ step.actionLabel || '验证' }}
                    </el-button>
                    <el-button
                      v-else-if="step.actionType === 'DOC' && step.docUrl"
                      size="small"
                      link
                      @click="openDoc(step.docUrl)"
                    >
                      {{ step.actionLabel || '文档' }}
                    </el-button>
                    <el-button
                      v-else-if="step.actionType === 'COMMAND' && step.commandExample"
                      size="small"
                      @click="copyCommand(step.commandExample)"
                    >
                      复制命令
                    </el-button>
                    <el-tag v-else-if="step.actionType === 'CONFIG'" size="small" type="info">
                      {{ step.actionLabel }}: {{ step.actionTarget }}
                    </el-tag>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>

          <div v-if="detail.troubleshooting?.length" class="table-panel">
            <h3 class="table-panel-title">常见问题</h3>
            <el-collapse>
              <el-collapse-item
                v-for="(item, idx) in detail.troubleshooting"
                :key="idx"
                :title="item.problem"
                :name="String(idx)"
              >
                <p class="ts-solution">{{ item.solution }}</p>
              </el-collapse-item>
            </el-collapse>
          </div>
        </el-col>

        <el-col :xs="24" :lg="10">
          <div v-if="detail.useCases?.length || detail.whenNotToUse?.length" class="table-panel">
            <h3 class="table-panel-title">适用场景</h3>
            <div v-if="detail.useCases?.length" class="scenario-block">
              <h4>适合使用</h4>
              <ul class="bullet-list">
                <li v-for="(item, idx) in detail.useCases" :key="'u' + idx">{{ item }}</li>
              </ul>
            </div>
            <div v-if="detail.whenNotToUse?.length" class="scenario-block">
              <h4>不建议使用</h4>
              <ul class="bullet-list muted-list">
                <li v-for="(item, idx) in detail.whenNotToUse" :key="'w' + idx">{{ item }}</li>
              </ul>
            </div>
          </div>

          <div v-if="detail.configRefs?.length" class="table-panel">
            <h3 class="table-panel-title">配置参考</h3>
            <el-table :data="detail.configRefs" size="small" stripe>
              <el-table-column prop="key" label="配置项" min-width="160">
                <template #default="{ row }">
                  <code class="cfg-key">{{ row.key }}</code>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="说明" min-width="140" />
              <el-table-column label="必填" width="56" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.required" size="small" type="danger">是</el-tag>
                  <span v-else class="text-muted">—</span>
                </template>
              </el-table-column>
              <el-table-column label="示例" min-width="120">
                <template #default="{ row }">
                  <code v-if="row.example" class="cfg-example">{{ row.example }}</code>
                  <span v-if="row.envVar" class="env-hint">env: {{ row.envVar }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="table-panel">
            <h3 class="table-panel-title">完整配置示例</h3>
            <div class="code-block">
              <pre>{{ detail.configExample || '暂无' }}</pre>
            </div>
          </div>

          <div v-if="detail.prerequisites?.length" class="table-panel">
            <h3 class="table-panel-title">前置条件</h3>
            <ul class="bullet-list">
              <li v-for="(item, idx) in detail.prerequisites" :key="idx">{{ item }}</li>
            </ul>
          </div>

          <div v-if="detail.relatedTemplates?.length" class="table-panel">
            <h3 class="table-panel-title">推荐场景模板</h3>
            <el-tag
              v-for="tpl in detail.relatedTemplates"
              :key="tpl"
              class="tpl-tag"
              @click="go('/scenario-templates')"
            >
              {{ tpl }}
            </el-tag>
          </div>

          <div v-if="detail.relatedPlugins?.length" class="table-panel">
            <h3 class="table-panel-title">同槽位替代插件</h3>
            <el-button
              v-for="key in detail.relatedPlugins"
              :key="key"
              link
              type="primary"
              class="related-link"
              @click="go(`/plugin-catalog/${encodeURIComponent(key)}`)"
            >
              {{ key }}
            </el-button>
          </div>

          <div v-if="detail.docLinks?.length" class="table-panel">
            <h3 class="table-panel-title">相关文档</h3>
            <div class="doc-links">
              <el-button
                v-for="(link, idx) in detail.docLinks"
                :key="idx"
                link
                type="primary"
                @click="openDoc(link.url)"
              >
                {{ link.label }}
              </el-button>
            </div>
          </div>

          <div class="table-panel">
            <h3 class="table-panel-title">Maven 构件</h3>
            <code>{{ detail.mavenArtifact || '-' }}</code>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi, type AdapterCatalogDetailVO } from '../api/admin'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const healthLoading = ref(false)
const setDefaultLoading = ref(false)
const detail = ref<AdapterCatalogDetailVO | null>(null)

const catalogKey = () => decodeURIComponent(String(route.params.catalogKey || ''))

const overviewParagraphs = computed(() => {
  const text = detail.value?.overview?.trim()
  if (!text) return []
  return text.split(/\n\s*\n/).map(p => p.trim()).filter(Boolean)
})

function tierLabel(tier: string) {
  const map: Record<string, string> = {
    small: 'Small Tier',
    medium: 'Medium Tier',
    large: 'Large Tier',
    all: '全 Tier'
  }
  return map[tier] || tier
}

async function load() {
  const key = catalogKey()
  if (!key) return
  loading.value = true
  try {
    detail.value = await adminApi.getAdapterCatalogDetail(key)
  } catch {
    ElMessage.error('插件不存在或加载失败')
    router.push('/plugin-catalog')
  } finally {
    loading.value = false
  }
}

async function runHealthCheck() {
  healthLoading.value = true
  try {
    detail.value = await adminApi.adapterCatalogHealthCheck(catalogKey())
    ElMessage.success(detail.value?.healthUp ? '健康探测通过' : '健康探测未通过')
  } finally {
    healthLoading.value = false
  }
}

async function setAsDefault() {
  if (!detail.value) return
  setDefaultLoading.value = true
  try {
    await adminApi.setDefaultAdapter(detail.value.spiType, detail.value.pluginId)
    ElMessage.success('已设为默认，请按配置示例更新 YAML 并重启')
    await load()
  } finally {
    setDefaultLoading.value = false
  }
}

function go(path: string) {
  if (path.startsWith('/')) {
    router.push(path)
  } else {
    openDoc(path)
  }
}

function openDoc(url: string) {
  if (url.startsWith('http')) {
    window.open(url, '_blank')
  } else if (url.startsWith('/')) {
    router.push(url)
  } else {
    ElMessage.info(`文档路径：${url}`)
  }
}

async function copyCommand(cmd: string) {
  try {
    await navigator.clipboard.writeText(cmd)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.info(cmd)
  }
}

watch(() => route.params.catalogKey, load)
onMounted(load)
</script>

<style scoped>
.header-actions {
  display: flex;
  gap: 8px;
}
.detail-header {
  margin-bottom: 16px;
}
.detail-title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.detail-title-row h2 {
  margin: 0 0 6px;
}
.detail-tagline {
  margin: 0 0 6px;
  font-size: 15px;
  color: var(--text-primary);
  font-weight: 500;
}
.detail-sub {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
}
.detail-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.detail-desc {
  margin: 12px 0;
  color: var(--text-secondary);
}
.detail-kv {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: var(--text-secondary);
}
.restart-alert {
  margin: 12px 0;
}
.overview-para {
  margin: 0 0 12px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.65;
}
.overview-para:last-child {
  margin-bottom: 0;
}
.architecture-block pre {
  font-family: ui-monospace, 'Cascadia Code', monospace;
  line-height: 1.5;
}
.step-card {
  padding-bottom: 8px;
}
.step-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.step-card p {
  margin: 0 0 8px;
  color: var(--text-secondary);
  font-size: 13px;
}
.step-hints {
  margin: 0 0 8px;
  padding-left: 18px;
  font-size: 12px;
  color: var(--text-muted);
}
.step-verify {
  font-size: 12px !important;
  color: #409eff !important;
  background: #ecf5ff;
  padding: 6px 10px;
  border-radius: 6px;
}
.step-actions {
  margin-top: 8px;
}
.code-block {
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 8px;
  padding: 12px;
  overflow-x: auto;
  font-size: 12px;
}
.code-block pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
.bullet-list {
  margin: 0;
  padding-left: 18px;
  color: var(--text-secondary);
  font-size: 13px;
}
.muted-list {
  color: var(--text-muted);
}
.scenario-block h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--text-primary);
}
.scenario-block + .scenario-block {
  margin-top: 16px;
}
.tpl-tag {
  margin: 0 6px 6px 0;
  cursor: pointer;
}
.related-link {
  display: block;
  text-align: left;
  margin-bottom: 4px;
}
.doc-links {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}
.cfg-key {
  font-size: 11px;
}
.cfg-example {
  font-size: 11px;
  display: block;
}
.env-hint {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}
.text-muted {
  color: var(--text-muted);
}
.ts-solution {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
}
</style>
