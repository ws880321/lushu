import { get } from '../../utils/api'

Page({
  data: { myRoutes: [], hotRoutes: [] },

  onShow() {
    this.loadMyRoutes()
    this.loadHotRoutes()
  },

  async loadMyRoutes() {
    try {
      const d = await get('/routes?size=20')
      this.setData({ myRoutes: d.content || [] })
    } catch (_) {}
  },

  async loadHotRoutes() {
    try {
      const data = await get('/templates/popular?limit=6')
      console.log('热门模板返回:', JSON.stringify(data))
      this.setData({ hotRoutes: data || [] })
    } catch (e) {
      console.error('加载热门模板失败', e)
    }
  },

  goGenerate() {
    wx.navigateTo({ url: '/pages/generate/generate' })
  },

  goRecord() {
    wx.navigateTo({ url: '/pages/record/record' })
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    if (id) {
      wx.navigateTo({ url: '/pages/route-detail/route-detail?id=' + id })
    }
  }
})
