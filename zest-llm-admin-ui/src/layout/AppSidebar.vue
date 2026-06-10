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

      <el-sub-menu index="platform">
        <template #title>
          <el-icon><Connection /></el-icon>
          <span>平台能力</span>
        </template>
        <el-menu-item index="/capability-stack">能力栈</el-menu-item>
        <el-menu-item index="/scenario-templates">场景模板</el-menu-item>
        <el-menu-item index="/adapters">适配器健康</el-menu-item>
        <el-menu-item index="/learning">自我改进</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="ai-jobs">
        <template #title>
          <el-icon><List /></el-icon>
          <span>AI 作业</span>
        </template>
        <el-menu-item index="/tasks">作业定义</el-menu-item>
        <el-menu-item index="/prompts">Prompt 管理</el-menu-item>
        <el-menu-item index="/model-routes">模型路由</el-menu-item>
        <el-menu-item index="/agent-config">智能体配置</el-menu-item>
        <el-menu-item index="/eval">Eval 评测</el-menu-item>
        <el-menu-item index="/executions">执行记录</el-menu-item>
        <el-menu-item index="/registry">方法注册</el-menu-item>
      </el-sub-menu>

      <el-menu-item index="/apps">
        <el-icon><Grid /></el-icon>
        <span>应用管理</span>
      </el-menu-item>
      <el-menu-item index="/tenants">
        <el-icon><OfficeBuilding /></el-icon>
        <span>租户管理</span>
      </el-menu-item>
      <el-menu-item index="/playground">
        <el-icon><Cpu /></el-icon>
        <span>Playground</span>
      </el-menu-item>
      <el-menu-item index="/flow-chains">
        <el-icon><Share /></el-icon>
        <span>Flow 链</span>
      </el-menu-item>
      <el-menu-item index="/users">
        <el-icon><User /></el-icon>
        <span>用户管理</span>
      </el-menu-item>
      <el-menu-item index="/audit-logs">
        <el-icon><Notebook /></el-icon>
        <span>审计日志</span>
      </el-menu-item>
      <el-menu-item index="/ops">
        <el-icon><Bell /></el-icon>
        <span>运维中心</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import {
  Bell,
  Connection,
  Cpu,
  Grid,
  List,
  Notebook,
  OfficeBuilding,
  Odometer,
  Share,
  User
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

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
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

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background-color: rgba(255, 255, 255, 0.06) !important;
}
</style>
