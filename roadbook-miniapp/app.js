App({
  globalData: {
    baseUrl: 'https://www.wychen.net/api/v1',
    token: null,
    userInfo: null
  },
  onLaunch() {
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
    }
  }
})
