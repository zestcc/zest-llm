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
            <p class="detail-sub">
              {{ detail.spiType }} · {{ detail.pluginId }} · {{ detail.vendor }} v{{ detail.version }}
            </p>
          </div>
          <div class="detail-tags">
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

      <el-row :gutter="16">
        <el-col :xs="24" :lg="14">
          <div class="table-panel">
            <h3 class="table-panel-title">分步集成指南</h3>
            <p class="table-panel-subtitle">按顺序完成以下步骤（参考成熟集成向导：配置 → 验证 → 跳转）</p>
            <el-timeline>
              <el-timeline-item
                v-for="step in detail?.integrationSteps || []"
                :key="step.stepId"
                :type="step.required ? 'primary' : 'info'"
              >
                <div class="step-card">
                  <div class="step-head">
                    <strong>{{ step.order }}. {{ step.title }}</strong>
                    <el-tag v-if="step.required" size="small" type="danger">必做</el-tag>
                  </div>
                  <p>{{ step.description }}</p>
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
                    <el-tag v-else-if="step.actionType === 'CONFIG'" size="small" type="info">
                      {{ step.actionLabel }}: {{ step.actionTarget }}
                    </el-tag>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-col>

        <el-col :xs="24" :lg="10">
          <div class="table-panel">
            <h3 class="table-panel-title">配置示例</h3>
            <div class="code-block">
              <pre>{{ detail?.configExample || '暂无' }}</pre>
            </div>
          </div>

          <div v-if="detail?.prerequisites?.length" class="table-panel">
            <h3 class="table-panel-title">前置条件</h3>
            <ul class="bullet-list">
              <li v-for="(item, idx) in detail.prerequisites" :key="idx">{{ item }}</li>
            </ul>
          </div>

          <div v-if="detail?.relatedTemplates?.length" class="table-panel">
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

          <div class="table-panel">
            <h3 class="table-panel-title">Maven 构件</h3>
            <code>{{ detail?.mavenArtifact || '-' }}</code>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
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

async function load() {
  const key = catalogKey()
  if (!key) return
  loading.value = true
  try {
    detail.value = await adminApi.getAdapterCatalogDetail(key)
  } catch (e: unknown) {
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
  router.push(path)
}

function openDoc(url: string) {
  if (url.startsWith('http')) {
    window.open(url, '_blank')
  } else {
    ElMessage.info(`文档路径：${url}`)
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
.tpl-tag {
  margin: 0 6px 6px 0;
  cursor: pointer;
}
</style>
