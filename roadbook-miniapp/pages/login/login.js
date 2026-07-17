import { post } from '../../utils/api'

Page({
  data: {
    loading: false
  },

  async handleLogin() {
    this.setData({ loading: true })
    try {
      const { code } = await wx.login()
      const result = await post('/auth/wechat-login', { code, nickname: '自驾用户' })
      wx.setStorageSync('token', result.token)
      getApp().globalData.token = result.token
      wx.redirectTo({ url: '/pages/index/index' })
    } catch (e) {
      wx.showToast({ title: '登录失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  }
})
