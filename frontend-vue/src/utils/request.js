import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from './auth'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 60000
})

request.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  error => {
    if (error.response) {
      const status = error.response.status
      const data = error.response.data

      if (status === 401) {
        removeToken()
        const message = data?.message || '登录已过期，请重新登录'
        ElMessage.error(message)
        setTimeout(() => {
          router.push('/login')
        }, 1500)
      } else {
        const message = data?.message || error.message || '请求失败'
        ElMessage.error(message)
      }
    } else {
      ElMessage.error(error.message || '网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default request
