import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import AppLayout from '../layout/AppLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import AppsView from '../views/AppsView.vue'
import TasksView from '../views/TasksView.vue'
import PromptsView from '../views/PromptsView.vue'
import ExecutionsView from '../views/ExecutionsView.vue'
import AdaptersView from '../views/AdaptersView.vue'
import ModelRoutesView from '../views/ModelRoutesView.vue'
import RegistryView from '../views/RegistryView.vue'
import AuditLogsView from '../views/AuditLogsView.vue'
import TenantsView from '../views/TenantsView.vue'
import AgentConfigView from '../views/AgentConfigView.vue'
import UsersView from '../views/UsersView.vue'
import PlaygroundView from '../views/PlaygroundView.vue'
import EvalView from '../views/EvalView.vue'
import FlowChainsView from '../views/FlowChainsView.vue'
import OpsView from '../views/OpsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    {
      path: '/',
      component: AppLayout,
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: DashboardView, meta: { title: '概览' } },
        { path: 'apps', component: AppsView, meta: { title: '应用管理' } },
        { path: 'tenants', component: TenantsView, meta: { title: '租户管理' } },
        { path: 'tasks', component: TasksView, meta: { title: 'AI 作业' } },
        { path: 'prompts', component: PromptsView, meta: { title: 'Prompt 管理' } },
        { path: 'playground', component: PlaygroundView, meta: { title: 'Playground' } },
        { path: 'eval', component: EvalView, meta: { title: 'Eval 评测' } },
        { path: 'flow-chains', component: FlowChainsView, meta: { title: 'Flow 链' } },
        { path: 'model-routes', component: ModelRoutesView, meta: { title: '模型路由' } },
        { path: 'agent-config', component: AgentConfigView, meta: { title: '智能体配置' } },
        { path: 'users', component: UsersView, meta: { title: '用户管理' } },
        { path: 'executions', component: ExecutionsView, meta: { title: '执行记录' } },
        { path: 'registry', component: RegistryView, meta: { title: '方法注册' } },
        { path: 'audit-logs', component: AuditLogsView, meta: { title: '审计日志' } },
        { path: 'ops', component: OpsView, meta: { title: '运维中心' } },
        { path: 'adapters', component: AdaptersView, meta: { title: '适配器健康' } }
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
