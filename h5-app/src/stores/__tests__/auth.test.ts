import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../auth'

vi.mock('../../utils/api', () => ({
  default: {
    post: vi.fn().mockResolvedValue({
      data: { code: 0, data: { token: 'test-token', userId: 1, nickname: 'test', phone: '13800138000' } },
    }),
  },
}))

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('initializes with null token when localStorage is empty', () => {
    const store = useAuthStore()
    expect(store.token).toBeNull()
    expect(store.userInfo).toBeNull()
  })

  it('initializes with token from localStorage', () => {
    localStorage.setItem('app_token', 'existing-token')
    const store = useAuthStore()
    expect(store.token).toBe('existing-token')
  })

  it('login sets token and userInfo', async () => {
    const store = useAuthStore()
    const result = await store.login('13800138000', '000000', 'test')
    expect(result.token).toBe('test-token')
    expect(store.token).toBe('test-token')
    expect(localStorage.getItem('app_token')).toBe('test-token')
  })

  it('logout clears token and userInfo', () => {
    const store = useAuthStore()
    store.token = 'some-token'
    store.userInfo = { token: 't', userId: 1, nickname: 'n', phone: '138' }
    localStorage.setItem('app_token', 'some-token')
    store.logout()
    expect(store.token).toBeNull()
    expect(store.userInfo).toBeNull()
    expect(localStorage.getItem('app_token')).toBeNull()
  })
})
