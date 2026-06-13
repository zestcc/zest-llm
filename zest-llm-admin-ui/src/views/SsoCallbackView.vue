<template>
  <div class="sso-callback-page">正在完成 SSO 登录...</div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi } from '../api/admin'

const route = useRoute()
const router = useRouter()

onMounted(async () => {
  const code = route.query.code as string
  const state = route.query.state as string
  if (!code || !state) {
    ElMessage.error('SSO 回调参数缺失')
    router.replace('/login')
    return
  }
  try {
    const result = await adminApi.oidcCallback(code, state)
    localStorage.setItem('zest-llm-token', result.token)
    localStorage.setItem('zest-llm-user', result.username || '')
    localStorage.setItem('zest-llm-role', result.role || 'ADMIN')
    localStorage.setItem('zest-llm-sso-login', 'true')
    ElMessage.success('SSO 登录成功')
    router.replace('/dashboard')
  } catch {
    router.replace('/login')
  }
})
</script>

<style scoped>
.sso-callback-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
}
</style>
