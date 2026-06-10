import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import AppLayout from '../layout/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    {
      path: '/',
      component: AppLayout,
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { title: '概览' } },
        { path: 'apps', component: () => import('../views/AppsView.vue'), meta: { title: '应用管理' } },
        { path: 'tenants', component: () => import('../views/TenantsView.vue'), meta: { title: '租户管理' } },
        { path: 'tasks', component: () => import('../views/TasksView.vue'), meta: { title: 'AI 作业' } },
        { path: 'prompts', component: () => import('../views/PromptsView.vue'), meta: { title: 'Prompt 管理' } },
        { path: 'playground', component: () => import('../views/PlaygroundView.vue'), meta: { title: 'Playground' } },
        { path: 'eval', component: () => import('../views/EvalView.vue'), meta: { title: 'Eval 评测' } },
        { path: 'flow-chains', component: () => import('../views/FlowChainsView.vue'), meta: { title: 'Flow 链' } },
        { path: 'model-routes', component: () => import('../views/ModelRoutesView.vue'), meta: { title: '模型路由' } },
        { path: 'agent-config', component: () => import('../views/AgentConfigView.vue'), meta: { title: '智能体配置' } },
        { path: 'users', component: () => import('../views/UsersView.vue'), meta: { title: '用户管理' } },
        { path: 'executions', component: () => import('../views/ExecutionsView.vue'), meta: { title: '执行记录' } },
        { path: 'registry', component: () => import('../views/RegistryView.vue'), meta: { title: '方法注册' } },
        { path: 'audit-logs', component: () => import('../views/AuditLogsView.vue'), meta: { title: '审计日志' } },
        { path: 'ops', component: () => import('../views/OpsView.vue'), meta: { title: '运维中心' } },
        { path: 'adapters', component: () => import('../views/AdaptersView.vue'), meta: { title: '适配器健康' } }
      ]
    }
  ]
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('zest-llm-token')
  if (!to.meta.public && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
