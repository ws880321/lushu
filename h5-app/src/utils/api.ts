import axios from 'axios'
import { showNotify } from 'vant'

const api = axios.create({ baseURL: '/api/v1', timeout: 30000 })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('app_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let isOffline = false

api.interceptors.response.use(
  (response) => {
    if (isOffline) { isOffline = false; showNotify({ type: 'success', message: '网络已恢复', duration: 2000 }) }
    return response
  },
  (error: any) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('app_token')
      window.location.hash = '#/login'
      return Promise.reject(error)
    }
    if (!error.response && error.code === 'ERR_NETWORK') {
      if (!isOffline) { isOffline = true; showNotify({ type: 'danger', message: '网络连接异常，请检查网络', duration: 3000 }) }
    }
    return Promise.reject(error)
  },
)

window.addEventListener('online', () => { isOffline = false; showNotify({ type: 'success', message: '网络已恢复', duration: 2000 }) })
window.addEventListener('offline', () => { isOffline = true; showNotify({ type: 'danger', message: '网络已断开', duration: 0 }) })

let onlineCBs: Array<() => void> = []
window.addEventListener('online', () => onlineCBs.forEach(fn => fn()))
export function onReconnect(fn: () => void): () => void {
  onlineCBs.push(fn)
  return () => { onlineCBs = onlineCBs.filter(f => f !== fn) }
}

export default api
