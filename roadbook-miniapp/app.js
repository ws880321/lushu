App({
  globalData: {
    baseUrl: 'http://192.168.42.191:8080/api/v1',
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
