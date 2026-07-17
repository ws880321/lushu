import { post } from '../../utils/api'

Page({
  data: {
    name: '', typeIdx: 0, dayNum: 1, note: '', lng: null, lat: null, saving: false,
    types: ['景点', '美食', '住宿', '加油', '拍照', '其他'],
    typesMap: { '景点': 0, '美食': 1, '住宿': 2, '加油': 3, '拍照': 4, '其他': 5 },
    typeCodes: ['scenic', 'food', 'hotel', 'gas', 'photo', 'custom'],
    records: [], routeId: null
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
  async save() {
    if (!this.data.name) { wx.showToast({ title: '请输入地点名', icon: 'none' }); return }
    this.setData({ saving: true })
    try {
      const result = await post('/routes/record', {
        name: this.data.name,
        type: this.data.typeCodes[this.data.typeIdx],
        dayNumber: this.data.dayNum,
        note: this.data.note,
        lng: this.data.lng, lat: this.data.lat
      })
      const records = [...this.data.records, { name: this.data.name, type: this.data.typeCodes[this.data.typeIdx], dayNumber: this.data.dayNum, note: this.data.note }]
      this.setData({ records, routeId: result.routeId, name: '', note: '', saving: false })
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
