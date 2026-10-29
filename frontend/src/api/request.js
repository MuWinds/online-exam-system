import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 统一请求封装：
 * 1. 自动携带 Authorization: Bearer <token>
 * 2. 统一解开 { code, message, data } 结构
 * 3. 401 清理登录态并跳转登录页
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 200) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络异常'
    const isLoginRequest = (error.config?.url || '').includes('/auth/login')

    if (status === 401) {
      localStorage.removeItem('token')
      if (!isLoginRequest) {
        ElMessage.error(message)
        if (!window.location.pathname.startsWith('/login')) {
          window.location.href = '/login'
        }
      }
      return Promise.reject(error)
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
