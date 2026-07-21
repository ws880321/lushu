import { defineStore } from 'pinia'
import api from '../utils/api'

interface UserInfo {
  token: string
  userId: number
  nickname: string
  phone: string
}

export const useAuthStore = defineStore('auth', {
  state: (): { token: string | null; userInfo: UserInfo | null } => ({
    token: localStorage.getItem('app_token') || null,
    userInfo: null,
  }),
  actions: {
    async sendCode(phone: string): Promise<void> {
      await api.post('/auth/send-code', { phone })
    },
    async login(phone: string, code: string, nickname: string): Promise<UserInfo> {
      const res = await api.post('/auth/phone-login', { phone, code, nickname })
      this.token = res.data.data.token
      this.userInfo = res.data.data
      localStorage.setItem('app_token', this.token as string)
      return res.data.data as UserInfo
    },
    logout(): void {
      this.token = null
      this.userInfo = null
      localStorage.removeItem('app_token')
      window.location.hash = '#/login'
    },
  },
})
