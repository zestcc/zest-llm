import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

interface HttpClient {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
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
    if (error.response?.status === 401) {
      localStorage.removeItem('zest-llm-token')
      localStorage.removeItem('zest-llm-user')
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login')
      }
      ElMessage.error(getErrorMessage(error.response?.data) || '登录已过期，请重新登录')
    } else if (error.response?.data) {
      ElMessage.error(getErrorMessage(error.response.data))
    } else {
      ElMessage.error('网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

const http = instance as HttpClient

export default http
