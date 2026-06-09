import http from './http'

export interface PageResult<T> {
  records: T[]
  total: number
  current?: number
  size?: number
}

export interface AdminLoginVO {
  token: string
  username: string
  expiresIn?: number
}

export interface AppVO {
  id: number
  appKey: string
  appName: string
  status: string
  createdAt?: string
}

export interface AppForm {
  appKey: string
  appName: string
  tenantCode?: string
  status?: string
}

export interface RotateTokenResult {
  appKey: string
  appToken: string
}

export interface TaskVO {
  id: number
  appKey?: string
  code: string
  name: string
  description?: string
  status: string
  createdAt?: string
}

export interface TaskForm {
  appKey: string
  code: string
  name: string
  description?: string
  status?: string
}

export interface PromptVersionVO {
  id: number
  version: string
  status: string
  templateBody?: string
  outputSchema?: string
  publishedAt?: string
  createdBy?: string
  createdAt?: string
}

export interface PromptVersionForm {
  version: string
  templateBody: string
  outputSchema?: string
}

export interface ExecutionVO {
  traceId: string
  taskCode?: string
  bizId?: string
  promptVersion?: string
  model?: string
  status: string
  inputJson?: string
  outputJson?: string
  errorCode?: string
  errorMessage?: string
  latencyMs?: number
  promptTokens?: number
  completionTokens?: number
  cost?: number
  createdAt?: string
}

export interface AdapterHealthVO {
  kind?: string
  configured?: string
  adapterId: string
  up: boolean
  message?: string
}

export interface TenantVO {
  id?: number
  tenantCode: string
  tenantName: string
  status?: string
  createdAt?: string
}

export interface TenantForm {
  tenantCode: string
  tenantName: string
}

export interface DashboardStats {
  apps?: number
  tasks?: number
  executions?: number
  success?: number
  failed?: number
  todayExecutions?: number
  avgLatencyMs?: number
  successRate?: number
}

export interface CostDayRow {
  date: string
  totalCost?: number
  callCount?: number
  promptTokens?: number
  completionTokens?: number
}

export interface RegistryMethodVO {
  id?: number
  appKey?: string
  code: string
  methodSignature?: string
  inputFields?: string
  outputClass?: string
  registeredAt?: string
}

export interface QuotaVO {
  dailyTokenLimit?: number | null
  qpsLimit?: number | null
  dailyCostLimit?: number | null
}

export interface AuditLogVO {
  id?: number
  actor?: string
  action?: string
  resourceType?: string
  resourceId?: string
  detailJson?: string
  createdAt?: string
}

export interface ModelRouteVO {
  primaryModel: string
  fallbackModels?: string
  maxTokens?: number | null
  temperature?: number | null
  timeoutMs?: number | null
  status?: string
}

export const adminApi = {
  login(username: string, password: string) {
    return http.post<AdminLoginVO>('/api/admin/auth/login', { username, password })
  },

  listApps(page = 1, size = 20) {
    return http.get<PageResult<AppVO> | AppVO[]>('/api/admin/apps', { params: { page, size } })
  },

  createApp(body: AppForm) {
    return http.post<AppVO>('/api/admin/apps', body)
  },

  updateApp(appKey: string, body: Partial<AppForm>) {
    return http.put<AppVO>(`/api/admin/apps/${appKey}`, body)
  },

  rotateToken(appKey: string) {
    return http.post<RotateTokenResult>(`/api/admin/apps/${appKey}/rotate-token`)
  },

  listTasks(page = 1, size = 50) {
    return http.get<PageResult<TaskVO> | TaskVO[]>('/api/admin/tasks', { params: { page, size } })
  },

  createTask(body: TaskForm) {
    return http.post<TaskVO>('/api/admin/tasks', body)
  },

  updateTask(code: string, body: Partial<TaskForm>) {
    return http.put<TaskVO>(`/api/admin/tasks/${code}`, body)
  },

  listPromptVersions(code: string) {
    return http.get<PromptVersionVO[]>(`/api/admin/prompts/${code}/versions`)
  },

  createPromptVersion(code: string, body: PromptVersionForm) {
    return http.post<PromptVersionVO>(`/api/admin/prompts/${code}/versions`, body)
  },

  publishPrompt(code: string, version: string, operator?: string) {
    return http.post(`/api/admin/prompts/${code}/publish`, { version, operator })
  },

  rollbackPrompt(code: string, version: string) {
    return http.post(`/api/admin/prompts/${code}/rollback`, { version })
  },

  listExecutions(page = 1, size = 20, taskCode?: string, status?: string) {
    return http.get<PageResult<ExecutionVO>>('/api/admin/executions', {
      params: { page, size, taskCode: taskCode || undefined, status: status || undefined }
    })
  },

  getExecution(traceId: string) {
    return http.get<ExecutionVO>(`/api/admin/executions/${traceId}`)
  },

  adapterHealth() {
    return http.get<AdapterHealthVO>('/api/admin/adapters/health')
  },

  listAdapterHealth() {
    return http.get<AdapterHealthVO[]>('/api/admin/adapters/health/all')
  },

  listTenants(page = 1, size = 50) {
    return http.get<PageResult<TenantVO> | TenantVO[]>('/api/admin/tenants', { params: { page, size } })
  },

  createTenant(body: TenantForm) {
    return http.post<TenantVO>('/api/admin/tenants', body)
  },

  dashboardStats() {
    return http.get<DashboardStats>('/api/admin/dashboard/stats')
  },

  dashboardCost(days = 7) {
    return http.get<CostDayRow[] | { days: CostDayRow[] }>('/api/admin/dashboard/cost', {
      params: { days }
    })
  },

  async listRegistryMethods(page = 1, size = 100) {
    const data = await http.get<PageResult<RegistryMethodVO>>('/api/admin/registry/methods', {
      params: { page, size }
    })
    return normalizePage(data, page, size).records
  },

  listAuditLogs(page = 1, size = 20, action?: string, resourceType?: string) {
    return http.get<PageResult<AuditLogVO>>('/api/admin/audit-logs', {
      params: { page, size, action: action || undefined, resourceType: resourceType || undefined }
    })
  },

  getQuota(appKey: string) {
    return http.get<QuotaVO>(`/api/admin/apps/${appKey}/quota`)
  },

  updateQuota(appKey: string, body: QuotaVO) {
    return http.put<QuotaVO>(`/api/admin/apps/${appKey}/quota`, body)
  },

  async getModelRoute(taskCode: string) {
    const list = await http.get<ModelRouteVO[]>('/api/admin/model-routes', { params: { taskCode } })
    return list?.[0] || { primaryModel: '', fallbackModels: '', maxTokens: 1024, temperature: 0.7, timeoutMs: 30000 }
  },

  updateModelRoute(taskCode: string, body: ModelRouteVO) {
    return http.put<ModelRouteVO>(`/api/admin/model-routes/${taskCode}`, body)
  }
}

export function normalizePage<T>(
  data: PageResult<T> | T[] | null | undefined,
  page: number,
  size: number
): PageResult<T> {
  if (!data) return { records: [], total: 0 }
  if (Array.isArray(data)) {
    const start = (page - 1) * size
    return { records: data.slice(start, start + size), total: data.length }
  }
  return {
    records: data.records || [],
    total: data.total ?? data.records?.length ?? 0
  }
}

export function normalizeCostRows(data: CostDayRow[] | { days: CostDayRow[] } | null | undefined): CostDayRow[] {
  if (!data) return []
  if (Array.isArray(data)) return data
  return data.days || []
}
