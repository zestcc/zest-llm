import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { clearAuthSession, isTokenExpired } from '../utils/auth'

export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

interface HttpClient {
  get<T = unknown>(url: string, config?: AxiosRequestConfig & { skipErrorToast?: boolean }): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig & { skipErrorToast?: boolean }): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig & { skipErrorToast?: boolean }): Promise<T>
  delete<T = unknown>(url: string, config?: AxiosRequestConfig & { skipErrorToast?: boolean }): Promise<T>
}

const instance: AxiosInstance = axios.create({ baseURL: '', timeout: 30000 })

instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('zest-llm-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function getErrorMessage(data: unknown): string {
  if (data && typeof data === 'object' && !Array.isArray(data)) {
    const body = data as Record<string, unknown>
    if (typeof body.message === 'string' && body.message) {
      return body.message
    }
  }
  return '请求失败'
}

function getErrorCode(data: unknown): string | undefined {
  if (data && typeof data === 'object' && !Array.isArray(data)) {
    const code = (data as Record<string, unknown>).errorCode
    return typeof code === 'string' ? code : undefined
  }
  return undefined
}

function redirectToLogin(message: string, skipToast: boolean): void {
  clearAuthSession()
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
  if (!skipToast) {
    ElMessage.error(message)
  }
}

function shouldTreat403AsAuthFailure(
  token: string | null,
  errorCode: string | undefined,
  method: string
): boolean {
  if (!token || isTokenExpired(token)) {
    return true
  }
  return errorCode === 'ACCESS_DENIED' && method === 'get'
}

instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data
    if (!body || typeof body !== 'object' || Array.isArray(body)) {
      return body
    }
    const { code, message, data } = body as ApiResult
    if (typeof code === 'number' && code === 200) {
      return data
    }
    if (typeof code === 'number' && code !== 200) {
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message || '请求失败'))
    }
    return body
  },
  (error) => {
    const skipToast = Boolean((error.config as AxiosRequestConfig & { skipErrorToast?: boolean })?.skipErrorToast)
    if (error.response?.status === 401) {
      redirectToLogin(getErrorMessage(error.response?.data) || '登录已过期，请重新登录', skipToast)
    } else if (error.response?.status === 403) {
      const token = localStorage.getItem('zest-llm-token')
      const method = (error.config?.method || 'get').toLowerCase()
      if (shouldTreat403AsAuthFailure(token, getErrorCode(error.response?.data), method)) {
        redirectToLogin(getErrorMessage(error.response?.data) || '登录已过期，请重新登录', skipToast)
      } else if (!skipToast) {
        ElMessage.error(getErrorMessage(error.response?.data) || '无访问权限')
      }
    } else if (!skipToast) {
      if (error.response?.data) {
        ElMessage.error(getErrorMessage(error.response.data))
      } else {
        ElMessage.error('网络异常，请稍后重试')
      }
    }
    return Promise.reject(error)
  }
)

const http = instance as HttpClient

export default http
