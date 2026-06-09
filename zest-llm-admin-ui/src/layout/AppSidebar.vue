<template>
  <div class="sidebar">
    <div class="logo">
      <img v-if="!collapsed" src="/logo.svg" alt="ZestLLM" class="logo-img" />
      <img v-else src="/favicon.svg" alt="ZestLLM" class="logo-icon" />
    </div>
    <el-menu
      class="sidebar-menu sidebar-scroll"
      :default-active="route.path"
      :collapse="collapsed"
      background-color="transparent"
      text-color="var(--sidebar-text)"
      active-text-color="#ffffff"
      router
      @select="handleMenuSelect"
    >
      <el-menu-item index="/dashboard">
        <el-icon><Odometer /></el-icon>
        <span>概览</span>
      </el-menu-item>
      <el-menu-item index="/apps">
        <el-icon><Grid /></el-icon>
        <span>应用管理</span>
      </el-menu-item>
      <el-menu-item index="/tenants">
        <el-icon><OfficeBuilding /></el-icon>
        <span>租户管理</span>
      </el-menu-item>
      <el-menu-item index="/tasks">
        <el-icon><List /></el-icon>
        <span>AI 作业</span>
      </el-menu-item>
      <el-menu-item index="/prompts">
        <el-icon><Document /></el-icon>
        <span>Prompt 管理</span>
      </el-menu-item>
      <el-menu-item index="/model-routes">
        <el-icon><Share /></el-icon>
        <span>模型路由</span>
      </el-menu-item>
      <el-menu-item index="/executions">
        <el-icon><Tickets /></el-icon>
        <span>执行记录</span>
      </el-menu-item>
      <el-menu-item index="/registry">
        <el-icon><Collection /></el-icon>
        <span>方法注册</span>
      </el-menu-item>
      <el-menu-item index="/audit-logs">
        <el-icon><Notebook /></el-icon>
        <span>审计日志</span>
      </el-menu-item>
      <el-menu-item index="/adapters">
        <el-icon><Connection /></el-icon>
        <span>适配器健康</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import {
  Collection,
  Connection,
  Document,
  Grid,
  List,
  Notebook,
  OfficeBuilding,
  Odometer,
  Share,
  Tickets
} from '@element-plus/icons-vue'

defineProps<{ collapsed: boolean }>()
const emit = defineEmits<{ navigate: [index: string] }>()

const route = useRoute()

function handleMenuSelect(index: string) {
  emit('navigate', index)
}
</script>

<style scoped>
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.logo {
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-img {
  height: 30px;
}

.logo-icon {
  height: 26px;
}

.sidebar-menu {
  border-right: none;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 0 12px;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 100%;
}

.sidebar-menu :deep(.el-menu-item) {
  margin: 2px 10px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.28) 0%, rgba(64, 158, 255, 0.12) 100%) !important;
  color: #ffffff !important;
  font-weight: 500;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.06) !important;
}
</style>
