<template>
  <div class="login-page">
    <div class="bg-decor">
      <div class="bg-grid" />
      <div class="bg-glow" />
      <div class="bg-flow tl">
        <svg viewBox="0 0 120 80" class="flow-svg">
          <g stroke="rgba(255,255,255,0.08)" stroke-width="1" fill="none">
            <path d="M15,40 L50,40 L50,15 L85,15" />
            <path d="M50,40 L50,65 L85,65" />
          </g>
          <g>
            <circle cx="15" cy="40" r="5" fill="rgba(255,255,255,0.06)" />
            <rect x="78" y="8" width="14" height="14" rx="3" fill="rgba(255,255,255,0.06)" />
            <rect x="78" y="58" width="14" height="14" rx="3" fill="rgba(255,255,255,0.04)" />
          </g>
        </svg>
      </div>
      <div class="bg-flow tr">
        <svg viewBox="0 0 100 70" class="flow-svg">
          <g stroke="rgba(255,255,255,0.08)" stroke-width="1" fill="none">
            <path d="M10,35 L45,35 L45,15 L80,15" />
            <path d="M45,35 L45,55 L80,55" />
          </g>
          <g>
            <circle cx="10" cy="35" r="4" fill="rgba(255,255,255,0.05)" />
            <rect x="74" y="8" width="12" height="14" rx="3" fill="rgba(255,255,255,0.06)" />
            <rect x="74" y="48" width="12" height="14" rx="3" fill="rgba(255,255,255,0.04)" />
          </g>
        </svg>
      </div>
      <div class="bg-flow ml">
        <svg viewBox="0 0 90 110" class="flow-svg">
          <g stroke="rgba(255,255,255,0.07)" stroke-width="1" fill="none">
            <path d="M15,15 L15,55 L50,55" />
            <path d="M15,55 L15,95 L50,95" />
            <path d="M15,35 L50,35" />
          </g>
          <g>
            <circle cx="15" cy="15" r="4" fill="rgba(255,255,255,0.05)" />
            <circle cx="15" cy="55" r="3" fill="rgba(255,255,255,0.04)" />
            <circle cx="15" cy="95" r="3" fill="rgba(255,255,255,0.04)" />
            <rect x="44" y="48" width="12" height="14" rx="3" fill="rgba(255,255,255,0.05)" />
            <rect x="44" y="88" width="12" height="14" rx="3" fill="rgba(255,255,255,0.04)" />
            <rect x="44" y="28" width="12" height="14" rx="3" fill="rgba(255,255,255,0.05)" />
          </g>
        </svg>
      </div>
      <div class="bg-flow mr">
        <svg viewBox="0 0 80 100" class="flow-svg">
          <g stroke="rgba(255,255,255,0.07)" stroke-width="1" fill="none">
            <path d="M10,20 L10,50 L45,50" />
            <path d="M10,80 L10,50" />
          </g>
          <g>
            <circle cx="10" cy="20" r="3.5" fill="rgba(255,255,255,0.05)" />
            <circle cx="10" cy="80" r="3" fill="rgba(255,255,255,0.04)" />
            <rect x="39" y="43" width="12" height="14" rx="3" fill="rgba(255,255,255,0.05)" />
          </g>
        </svg>
      </div>
      <div class="bg-flow br">
        <svg viewBox="0 0 180 120" class="flow-svg">
          <g stroke="rgba(255,255,255,0.12)" stroke-width="1.2" fill="none">
            <path d="M25,25 L65,25 L65,60 L105,60" />
            <path d="M25,25 L25,95 L65,95 L65,60" />
            <path d="M105,60 L145,60 L145,25" />
            <path d="M105,60 L105,95 L145,95" />
          </g>
          <g>
            <circle cx="25" cy="25" r="8" fill="rgba(255,255,255,0.08)" />
            <circle cx="25" cy="95" r="6" fill="rgba(255,255,255,0.05)" />
            <rect x="93" y="50" width="24" height="20" rx="5" fill="rgba(255,255,255,0.08)" />
            <rect x="133" y="15" width="20" height="20" rx="5" fill="rgba(255,255,255,0.05)" />
            <rect x="133" y="85" width="20" height="20" rx="5" fill="rgba(255,255,255,0.05)" />
          </g>
        </svg>
      </div>
      <div class="bg-flow bl">
        <svg viewBox="0 0 130 90" class="flow-svg">
          <g stroke="rgba(255,255,255,0.09)" stroke-width="1" fill="none">
            <path d="M15,45 L50,45 L50,20 L85,20" />
            <path d="M50,45 L50,70 L85,70" />
            <path d="M15,45 L15,20 L50,20" />
          </g>
          <g>
            <circle cx="15" cy="45" r="6" fill="rgba(255,255,255,0.06)" />
            <circle cx="15" cy="20" r="4" fill="rgba(255,255,255,0.04)" />
            <rect x="76" y="12" width="16" height="16" rx="4" fill="rgba(255,255,255,0.06)" />
            <rect x="76" y="62" width="16" height="16" rx="4" fill="rgba(255,255,255,0.04)" />
          </g>
        </svg>
      </div>
    </div>

    <div class="login-card">
      <div class="card-logo">
        <img src="/favicon.svg" alt="ZestLLM" />
      </div>
      <h2 class="card-title">ZestLLM Admin</h2>
      <p class="card-subtitle">AI 作业调度与治理控制台</p>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        size="large"
        @keyup.enter="submit"
      >
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item prop="captcha">
          <div class="captcha-row">
            <el-input
              v-model="form.captcha"
              placeholder="验证码"
              maxlength="4"
              class="captcha-input"
            />
            <Captcha ref="captchaRef" @update:code="captchaCode = $event" />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="btn-login" @click="submit">
            {{ loading ? '登录中…' : '登 录' }}
          </el-button>
        </el-form-item>
        <el-form-item v-if="ssoEnabled">
          <el-button class="btn-sso" :loading="ssoLoading" @click="handleSsoLogin">
            ZestSSO 登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <el-tooltip content="本地开发默认账号：admin / admin123" placement="top">
          <span class="login-footer-tip">
            <el-icon><InfoFilled /></el-icon>
            登录说明
          </span>
        </el-tooltip>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { InfoFilled, Lock, User } from '@element-plus/icons-vue'
import Captcha from '../components/Captcha.vue'
import { adminApi } from '../api/admin'

const router = useRouter()
const formRef = ref<FormInstance>()
const captchaRef = ref<InstanceType<typeof Captcha>>()
const captchaCode = ref('')
const loading = ref(false)
const ssoLoading = ref(false)
const ssoEnabled = ref(false)
const form = reactive({ username: 'admin', password: 'admin123', captcha: '' })

onMounted(() => {
  adminApi.getOidcConfig().then((cfg) => {
    ssoEnabled.value = !!cfg?.enabled
  }).catch(() => {
    ssoEnabled.value = false
  })
})

async function handleSsoLogin() {
  ssoLoading.value = true
  try {
    const res = await adminApi.getOidcAuthorize()
    if (res?.authorizationUrl) {
      window.location.href = res.authorizationUrl
    }
  } catch {
    ElMessage.error('SSO 登录初始化失败')
  } finally {
    ssoLoading.value = false
  }
}

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码为 4 位', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value && value.toLowerCase() !== captchaCode.value.toLowerCase()) {
          callback(new Error('验证码错误'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (form.captcha.toLowerCase() !== captchaCode.value.toLowerCase()) {
    ElMessage.error('验证码错误')
    captchaRef.value?.refresh()
    return
  }

  loading.value = true
  try {
    const result = await adminApi.login(form.username, form.password)
    localStorage.setItem('zest-llm-token', result.token)
    localStorage.setItem('zest-llm-user', result.username || form.username)
    localStorage.setItem('zest-llm-role', result.role || 'ADMIN')
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brand-gradient);
  position: relative;
  overflow: hidden;
}

.bg-decor {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 40px 40px;
}

.bg-glow {
  position: absolute;
  top: -20%;
  left: 50%;
  transform: translateX(-50%);
  width: 700px;
  height: 700px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 60%);
  border-radius: 50%;
}

.bg-flow {
  position: absolute;
}

.bg-flow.tl { top: 40px; left: 50px; width: 100px; }
.bg-flow.tr { top: 130px; right: 70px; width: 85px; }
.bg-flow.ml { top: 38%; left: 30px; width: 95px; }
.bg-flow.mr { top: 52%; right: 50px; width: 75px; }
.bg-flow.bl { bottom: 60px; left: 40px; width: 110px; }
.bg-flow.br { bottom: 30px; right: 80px; width: 130px; }

.flow-svg {
  width: 100%;
  height: auto;
}

.login-card {
  width: 400px;
  padding: 40px 40px 32px;
  background: #fff;
  border-radius: 16px;
  box-shadow: var(--shadow-lg);
  position: relative;
  z-index: 1;
}

.card-logo {
  text-align: center;
  margin-bottom: 16px;
}

.card-logo img {
  width: 48px;
  height: 48px;
}

.card-title {
  text-align: center;
  margin: 0 0 4px;
  font-size: 22px;
  color: #1d2129;
  font-weight: 600;
}

.card-subtitle {
  text-align: center;
  margin: 0 0 28px;
  font-size: 14px;
  color: #86909c;
}

.login-card :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #d9dde3, inset 0 0 0 1000px #fff !important;
  border-radius: 8px;
}

.login-card :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #667eea, inset 0 0 0 1000px #fff !important;
}

.login-card :deep(.el-input__inner) {
  caret-color: #333;
}

.login-card :deep(.el-input__inner:-webkit-autofill),
.login-card :deep(.el-input__inner:-webkit-autofill:hover),
.login-card :deep(.el-input__inner:-webkit-autofill:focus) {
  -webkit-box-shadow: 0 0 0 1000px #fff inset !important;
  box-shadow: 0 0 0 1000px #fff inset !important;
  -webkit-text-fill-color: #333 !important;
}

.login-card :deep(.el-input__prefix-inner .el-icon) {
  color: #c9cdd4;
}

.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha-input {
  flex: 1;
}

.btn-login {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
}

.btn-sso {
  width: 100%;
  height: 44px;
  border-radius: 8px;
}

.login-footer {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.login-footer-tip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #86909c;
  cursor: help;
}

@media (max-width: 767px) {
  .login-page {
    padding: 20px;
  }

  .login-card {
    width: 100%;
    padding: 28px 20px 24px;
    border-radius: 12px;
  }

  .card-title {
    font-size: 20px;
  }

  .card-subtitle {
    font-size: 13px;
    margin-bottom: 20px;
  }

  .bg-flow.tl { top: 20px; left: 10px; width: 60px; }
  .bg-flow.tr { top: 80px; right: 20px; width: 55px; }
  .bg-flow.ml { display: none; }
  .bg-flow.mr { display: none; }
  .bg-flow.bl { bottom: 20px; left: 10px; width: 70px; }
  .bg-flow.br { bottom: 10px; right: 30px; width: 80px; }
}
</style>
