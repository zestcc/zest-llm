<template>
  <el-container class="layout-container">
    <el-drawer
      v-if="isMobile"
      v-model="sidebarOpen"
      :size="mobileSidebarSize"
      :with-header="false"
      :close-on-click-modal="true"
      direction="ltr"
      class="mobile-sidebar-drawer"
      append-to-body
    >
      <AppSidebar :collapsed="false" @navigate="closeMobileSidebar" />
    </el-drawer>

    <el-aside v-else :width="collapsed ? '64px' : '220px'" class="desktop-sidebar">
      <AppSidebar :collapsed="collapsed" />
    </el-aside>

    <el-container class="main-container">
      <el-header>
        <AppHeader
          :is-mobile="isMobile"
          :sidebar-open="sidebarOpen"
          :collapsed="collapsed"
          @toggle-sidebar="toggleSidebar"
        />
      </el-header>
      <el-main class="main-content content-scroll">
        <div v-if="route.meta?.title" class="page-title">{{ route.meta.title }}</div>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'

const route = useRoute()
const isMobile = ref(false)
const sidebarOpen = ref(false)
const collapsed = ref(false)

const mobileSidebarSize = computed(() => (isMobile.value ? 'min(280px, 85vw)' : 260))

function checkMobile() {
  isMobile.value = window.innerWidth < 768
  if (!isMobile.value) sidebarOpen.value = false
}

function closeMobileSidebar() {
  sidebarOpen.value = false
}

function toggleSidebar() {
  if (isMobile.value) {
    sidebarOpen.value = !sidebarOpen.value
  } else {
    collapsed.value = !collapsed.value
  }
}

watch(
  () => route.path,
  () => {
    if (isMobile.value) closeMobileSidebar()
  }
)

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background: var(--main-bg);
}

.desktop-sidebar {
  background: linear-gradient(180deg, var(--sidebar-bg) 0%, var(--sidebar-bg-end) 100%);
  transition: width 0.28s ease;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: 100%;
  box-shadow: 1px 0 0 rgba(0, 0, 0, 0.06);
}

.el-header {
  background-color: var(--surface-bg);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  padding: 0 20px;
  height: 56px;
  box-shadow: var(--shadow-sm);
}

.main-content {
  background-color: var(--main-bg);
  padding: 20px 24px;
  overflow-y: auto;
}

.page-title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 20px;
  letter-spacing: -0.01em;
}

@media (max-width: 767px) {
  .el-header {
    padding: 0 12px;
    height: 52px;
  }

  .main-content {
    padding: 12px;
  }

  .page-title {
    font-size: 16px;
    margin-bottom: 16px;
  }
}

:global(.mobile-sidebar-drawer.el-drawer) {
  max-width: 100vw;
  height: 100% !important;
}

:global(.mobile-sidebar-drawer .el-drawer__body) {
  padding: 0;
  background: linear-gradient(180deg, var(--sidebar-bg) 0%, var(--sidebar-bg-end) 100%);
  height: 100%;
  overflow: hidden;
}
</style>
