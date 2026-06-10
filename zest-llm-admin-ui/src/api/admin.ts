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
  role?: string
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
  observabilityAdapter?: string
  observabilityTraceUrl?: string
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
  agentsMonitored?: number
  agentsReady?: number
  agentsDegraded?: number
  agentsUnavailable?: number
  agentsUnknown?: number
}

export interface AgentHealthItem {
  taskCode?: string
  profileVersion?: string
  overallStatus?: string
  ready?: boolean
  latencyMs?: number
  probedAt?: string
  probeSource?: string
}

export interface AgentHealthDashboard {
  monitored?: number
  ready?: number
  degraded?: number
  unavailable?: number
  unknown?: number
  alerts?: AgentHealthItem[]
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
  alertWebhookUrl?: string | null
  alertThresholdPct?: number | null
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

export interface AgentProfileVO {
  id?: number
  taskCode?: string
  version: string
  profileJson?: string
  providerPresetCode?: string
  runtimeMode?: string
  status?: string
  publishedAt?: string
}

export interface AgentProfileProbeCheckVO {
  name: string
  category?: string
  critical?: boolean
  up: boolean
  message?: string
}

export interface AgentProfileProbeResultVO {
  probeId?: number
  taskCode?: string
  profileVersion?: string
  profileStatus?: string
  overallStatus?: string
  ready?: boolean
  latencyMs?: number
  probeSource?: string
  probedAt?: string
  checks?: AgentProfileProbeCheckVO[]
}

export interface AgentProfileProbeRequest {
  appKey?: string
  smokeTest?: boolean
}

export interface ProviderPresetVO {
  id?: number
  presetCode: string
  presetName: string
  providerType?: string
  authMode?: string
  configJson?: string
  sortOrder?: number
  status?: string
}

export interface VersionDiffVO {
  fromVersion?: string
  toVersion?: string
  changes?: DiffEntryVO[]
}

export interface DiffEntryVO {
  field?: string
  changeType?: string
  before?: string
  after?: string
  unifiedDiff?: string
}

export interface AuthBindingVO {
  scopeType?: string
  scopeId?: number
  appKey?: string
  inboundMode?: string
  inboundConfigJson?: string
  outboundMode?: string
  outboundConfigJson?: string
}

export interface McpServerVO {
  id?: number
  serverCode: string
  serverName: string
  baseUrl: string
  authSecretRef?: string
  configJson?: string
  status?: string
}

export interface McpServerForm {
  serverCode: string
  serverName: string
  baseUrl: string
  authSecretRef?: string
  configJson?: string
  status?: string
}

export interface McpToolDescriptor {
  name: string
  description?: string
  inputSchema?: unknown
}

export interface AdminUserVO {
  id?: number
  username: string
  displayName?: string
  role?: string
  status?: string
  createdAt?: string
}

export interface PlaygroundPreviewVO {
  traceId?: string
  code?: string
  promptVersion?: string
  renderedPrompt?: string
  model?: string
}

export interface PlaygroundRunVO {
  traceId?: string
  status?: string
  code?: string
  output?: Record<string, unknown>
  metrics?: { latencyMs?: number; cacheHit?: boolean }
  cacheHit?: boolean
  errorCode?: string
  errorMessage?: string
}

export interface EvalDatasetVO {
  id?: number
  datasetCode: string
  datasetName: string
  appKey: string
  taskCode: string
  status?: string
}

export interface EvalRunVO {
  runCode: string
  datasetCode?: string
  status?: string
  totalCases?: number
  passedCases?: number
  failedCases?: number
  passRate?: number
  caseResults?: Array<Record<string, unknown>>
  startedAt?: string
  finishedAt?: string
}

export interface FlowChainVO {
  id?: number
  chainCode: string
  chainName?: string
  version?: number
  lifecycle?: string
  chainData?: string
  status?: string
  updatedAt?: string
}

export interface ExecutionArchiveStatsVO {
  hotExecutions?: number
  archivedExecutions?: number
  retentionDays?: number
  archiveEnabled?: boolean
  lastRunAt?: string
  lastArchivedCount?: number
  lastDeletedCount?: number
}

export interface ObservabilityConfigVO {
  adapterId?: string
  langfuseEnabled?: boolean
  langfuseUiBaseUrl?: string
  tracePathTemplate?: string
}

export interface CostAlertVO {
  appKey?: string
  alertDate?: string
  dailyCost?: number
  costLimit?: number
  thresholdPct?: number
  status?: string
  message?: string
  createdAt?: string
}

export interface AgentProbeAlertVO {
  taskCode?: string
  profileVersion?: string
  overallStatus?: string
  probeId?: number
  status?: string
  message?: string
  createdAt?: string
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

  diffPrompt(code: string, from: string, to: string) {
    return http.get<VersionDiffVO>(`/api/admin/prompts/${code}/diff`, { params: { from, to } })
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
  },

  listAgentProfiles(taskCode: string) {
    return http.get<AgentProfileVO[]>(`/api/admin/agent-profiles/${taskCode}/versions`)
  },

  createAgentProfile(taskCode: string, body: Partial<AgentProfileVO> & { profileJson: string; version: string }) {
    return http.post<AgentProfileVO>(`/api/admin/agent-profiles/${taskCode}/versions`, body)
  },

  updateAgentProfile(taskCode: string, version: string, body: Partial<AgentProfileVO> & { profileJson: string }) {
    return http.put<AgentProfileVO>(`/api/admin/agent-profiles/${taskCode}/versions/${version}`, body)
  },

  publishAgentProfile(taskCode: string, version: string, operator?: string) {
    return http.post(`/api/admin/agent-profiles/${taskCode}/publish`, { version, operator })
  },

  rollbackAgentProfile(taskCode: string, version: string) {
    return http.post(`/api/admin/agent-profiles/${taskCode}/rollback`, { version })
  },

  diffAgentProfile(taskCode: string, from: string, to: string) {
    return http.get<VersionDiffVO>(`/api/admin/agent-profiles/${taskCode}/diff`, { params: { from, to } })
  },

  importAgentProfile(body: { taskCode: string; profileJson: string; publish?: boolean; version?: string }) {
    return http.post<AgentProfileVO>('/api/admin/agent-profiles/import', body)
  },

  exportAgentProfile(taskCode: string, version: string) {
    return http.get<{ profileJson: string }>(`/api/admin/agent-profiles/${taskCode}/versions/${version}/export`)
  },

  activateAgentProvider(taskCode: string, providerRef: string) {
    return http.post(`/api/admin/agent-profiles/${taskCode}/activate-provider`, { providerRef })
  },

  probeAgentProfilePublished(taskCode: string, body?: AgentProfileProbeRequest) {
    return http.post<AgentProfileProbeResultVO>(`/api/admin/agent-profiles/${taskCode}/probe`, body ?? {})
  },

  probeAllAgentProfiles(body?: AgentProfileProbeRequest) {
    return http.post<{ probedCount: number }>('/api/admin/agent-profiles/probe-all', body ?? {})
  },

  probeAgentProfileVersion(taskCode: string, version: string, body?: AgentProfileProbeRequest) {
    return http.post<AgentProfileProbeResultVO>(
      `/api/admin/agent-profiles/${taskCode}/versions/${version}/probe`,
      body ?? {}
    )
  },

  getAgentProfileProbeLatest(taskCode: string) {
    return http.get<AgentProfileProbeResultVO | null>(`/api/admin/agent-profiles/${taskCode}/probe/latest`)
  },

  listAgentProfileProbeHistory(taskCode: string, page = 1, size = 20, profileVersion?: string) {
    return http.get<PageResult<AgentProfileProbeResultVO>>(
      `/api/admin/agent-profiles/${taskCode}/probe/history`,
      { params: { page, size, profileVersion } }
    )
  },

  dashboardAgentHealth() {
    return http.get<AgentHealthDashboard>('/api/admin/dashboard/agent-health')
  },

  listProviderPresets() {
    return http.get<ProviderPresetVO[]>('/api/admin/provider-presets')
  },

  createProviderPreset(body: ProviderPresetVO) {
    return http.post<ProviderPresetVO>('/api/admin/provider-presets', body)
  },

  updateProviderPreset(presetCode: string, body: Partial<ProviderPresetVO>) {
    return http.put<ProviderPresetVO>(`/api/admin/provider-presets/${presetCode}`, body)
  },

  getAuthBinding(appKey: string) {
    return http.get<AuthBindingVO>(`/api/admin/auth-bindings/apps/${appKey}`)
  },

  upsertAuthBinding(body: {
    scopeType: string
    appKey?: string
    inboundMode: string
    inboundConfigJson?: string
    outboundMode?: string
    outboundConfigJson?: string
  }) {
    return http.put<AuthBindingVO>('/api/admin/auth-bindings', body)
  },

  listMcpServers() {
    return http.get<McpServerVO[]>('/api/admin/mcp-servers')
  },

  createMcpServer(body: McpServerForm) {
    return http.post<McpServerVO>('/api/admin/mcp-servers', body)
  },

  updateMcpServer(serverCode: string, body: Partial<McpServerForm>) {
    return http.put<McpServerVO>(`/api/admin/mcp-servers/${serverCode}`, body)
  },

  listMcpServerTools(serverCode: string) {
    return http.get<McpToolDescriptor[]>(`/api/admin/mcp-servers/${serverCode}/tools`)
  },

  listAdminUsers() {
    return http.get<AdminUserVO[]>('/api/admin/users')
  },

  createAdminUser(body: { username: string; password: string; displayName?: string; role?: string }) {
    return http.post<AdminUserVO>('/api/admin/users', body)
  },

  updateAdminUser(username: string, body: { displayName?: string; password?: string; role?: string; status?: string }) {
    return http.put<AdminUserVO>(`/api/admin/users/${username}`, body)
  },

  playgroundPreview(body: { appKey: string; code: string; inputs?: Record<string, unknown> }) {
    return http.post<PlaygroundPreviewVO>('/api/admin/playground/preview', body)
  },

  playgroundRun(body: { appKey: string; code: string; inputs?: Record<string, unknown>; bizId?: string }) {
    return http.post<PlaygroundRunVO>('/api/admin/playground/run', body)
  },

  listEvalDatasets() {
    return http.get<EvalDatasetVO[]>('/api/admin/eval/datasets')
  },

  listEvalRuns(datasetCode: string) {
    return http.get<EvalRunVO[]>(`/api/admin/eval/datasets/${datasetCode}/runs`)
  },

  runEvalDataset(datasetCode: string) {
    return http.post<EvalRunVO>(`/api/admin/eval/datasets/${datasetCode}/run`)
  },

  getEvalRun(runCode: string) {
    return http.get<EvalRunVO>(`/api/admin/eval/runs/${runCode}`)
  },

  createEvalDataset(body: {
    datasetCode: string
    datasetName: string
    appKey: string
    taskCode: string
  }) {
    return http.post<EvalDatasetVO>('/api/admin/eval/datasets', body)
  },

  listFlowChains() {
    return http.get<FlowChainVO[]>('/api/admin/flow-chains')
  },

  getFlowChain(chainCode: string) {
    return http.get<FlowChainVO>(`/api/admin/flow-chains/${chainCode}`)
  },

  getExecutionArchiveStats() {
    return http.get<ExecutionArchiveStatsVO>('/api/admin/executions/archive/stats')
  },

  runExecutionArchive() {
    return http.post<ExecutionArchiveStatsVO>('/api/admin/executions/archive/run')
  },

  listCostAlerts(appKey?: string, page = 1, size = 20) {
    return http.get<PageResult<CostAlertVO>>('/api/admin/cost-alerts', { params: { appKey, page, size } })
  },

  listAgentProbeAlerts(taskCode?: string, page = 1, size = 20) {
    return http.get<PageResult<AgentProbeAlertVO>>('/api/admin/agent-probe-alerts', { params: { taskCode, page, size } })
  },

  getObservabilityConfig() {
    return http.get<ObservabilityConfigVO>('/api/admin/config/observability')
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
