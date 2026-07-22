import { post } from '../../utils/api'

Page({
  data: {
    name: '', typeIdx: 0, dayNum: 1, note: '', lng: null, lat: null, saving: false,
    types: ['景点', '美食', '住宿', '加油', '拍照', '其他'],
    typesMap: { '景点': 0, '美食': 1, '住宿': 2, '加油': 3, '拍照': 4, '其他': 5 },
    typeCodes: ['scenic', 'food', 'hotel', 'gas', 'photo', 'custom'],
    records: [], routeId: null,
    photos: [], photoUrl: null
  },
  onTypeChg(e) { this.setData({ typeIdx: parseInt(e.detail.value) }) },
  dayDown() { if (this.data.dayNum > 1) this.setData({ dayNum: this.data.dayNum - 1 }) },
  dayUp() { this.setData({ dayNum: this.data.dayNum + 1 }) },
  getLoc() {
    wx.getLocation({ type: 'gcj02', success: (res) => {
      this.setData({ lng: res.longitude, lat: res.latitude })
      wx.showToast({ title: '已定位', icon: 'success' })
    }, fail: () => wx.showToast({ title: '定位失败', icon: 'none' }) })
  },
  choosePhoto() {
    const that = this
    wx.chooseImage({
      count: 1, sizeType: ['compressed'], sourceType: ['camera', 'album'],
      success(res) { that.setData({ photos: res.tempFilePaths }) }
    })
  },
  removePhoto() {
    this.setData({ photos: [], photoUrl: null })
  },
  async save() {
    if (!this.data.name) { wx.showToast({ title: '请输入地点名', icon: 'none' }); return }
    this.setData({ saving: true })
    try {
      let photoUrl = null
      if (this.data.photos.length > 0) {
        const app = getApp()
        const uploadRes = await new Promise((resolve, reject) => {
          wx.uploadFile({
            url: (app.globalData.baseUrl || '') + '/upload',
            filePath: this.data.photos[0],
            name: 'file',
            header: app.globalData.token ? { 'Authorization': 'Bearer ' + app.globalData.token } : {},
            success(res) { try { resolve(JSON.parse(res.data)) } catch { resolve(null) } },
            fail: reject
          })
        })
        photoUrl = uploadRes && uploadRes.data ? uploadRes.data.url : null
      }
      const result = await post('/routes/record', {
        name: this.data.name,
        type: this.data.typeCodes[this.data.typeIdx],
        dayNumber: this.data.dayNum,
        note: this.data.note,
        lng: this.data.lng, lat: this.data.lat,
        photoUrl: photoUrl
      })
      const records = [...this.data.records, {
        name: this.data.name,
        type: this.data.typeCodes[this.data.typeIdx],
        dayNumber: this.data.dayNum,
        note: this.data.note,
        photo: this.data.photos[0] || null
      }]
      this.setData({ records, routeId: result.routeId, name: '', note: '', saving: false, photos: [], photoUrl: null })
      wx.showToast({ title: '已记录', icon: 'success' })
    } catch (e) {
      wx.showToast({ title: '保存失败', icon: 'none' })
      this.setData({ saving: false })
    }
  },
  async finish() {
    if (!this.data.routeId) return
    try {
      await post('/routes/' + this.data.routeId + '/finish', {})
      wx.showToast({ title: '行程已保存', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 1500)
    } catch (e) { wx.showToast({ title: '保存失败', icon: 'none' }) }
  }
})
