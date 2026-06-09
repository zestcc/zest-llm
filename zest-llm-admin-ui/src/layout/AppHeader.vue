<template>
  <div class="header">
    <el-button text @click="$emit('toggle-sidebar')">
      <el-icon :size="20">
        <Fold v-if="showFoldIcon" />
        <Expand v-else />
      </el-icon>
    </el-button>
    <div class="header-right">
      <el-tag type="info" effect="plain" size="small" class="env-tag hide-on-mobile">Control Plane</el-tag>
      <el-dropdown trigger="click">
        <span class="user-info">
          <el-avatar :size="32" class="user-avatar">
            {{ username.charAt(0).toUpperCase() }}
          </el-avatar>
          <span class="user-name hide-on-mobile">{{ username }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="openSwagger">API 文档</el-dropdown-item>
            <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, Expand, Fold } from '@element-plus/icons-vue'

const props = defineProps<{
  collapsed?: boolean
  isMobile?: boolean
  sidebarOpen?: boolean
}>()

defineEmits<{ 'toggle-sidebar': [] }>()

const router = useRouter()

const showFoldIcon = computed(() => {
  if (props.isMobile) return props.sidebarOpen
  return !props.collapsed
})

const username = computed(() => localStorage.getItem('zest-llm-user') || 'admin')

function logout() {
  localStorage.removeItem('zest-llm-token')
  localStorage.removeItem('zest-llm-user')
  router.push('/login')
}

function openSwagger() {
  window.open('/swagger-ui.html', '_blank')
}
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.env-tag {
  border-radius: 999px;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 8px 2px 2px;
  border-radius: 20px;
  transition: background-color 0.2s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.user-avatar {
  flex-shrink: 0;
  background: var(--brand-gradient);
  color: #fff;
  font-weight: 700;
  font-size: 14px;
  border: 2px solid #e8eaed;
}

.user-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 767px) {
  .header-right {
    gap: 8px;
  }
}
</style>
