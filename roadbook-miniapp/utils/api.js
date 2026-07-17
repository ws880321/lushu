const app = getApp()

function request(method, path, data) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: app.globalData.baseUrl + path,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        'Authorization': app.globalData.token ? `Bearer ${app.globalData.token}` : ''
      },
      success(res) {
        if (res.data.code === 0) {
          resolve(res.data.data)
        } else if (res.data.code === 40100) {
          wx.removeStorageSync('token')
          wx.reLaunch({ url: '/pages/login/login' })
          reject(res.data)
        } else {
          reject(res.data)
        }
      },
      fail(err) {
        reject(err)
      }
    })
  })
}

export const get = (path, data) => request('GET', path, data)
export const post = (path, data) => request('POST', path, data)
