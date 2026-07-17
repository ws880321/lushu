import { get, post } from '../../utils/api'
import { cacheRoute } from '../../utils/cache'

Page({
  data: {
    startName: '',
    startLng: null,
    startLat: null,
    endName: '',
    endLng: null,
    endLat: null,
    totalDays: 3,
    difficulty: 'easy',
    tags: [],
    dailyHours: 4,
    generating: false,
    popularTemplates: [],
    vehicleInfo: null,
    allTags: ['📷摄影', '⛺露营', '👶亲子', '🍜美食', '🏔️越野']
  },

  onLoad() {
    this.loadPopular()
    this.loadVehicleInfo()
  },

  async loadPopular() {
    try {
      this.setData({ popularTemplates: await get('/templates/popular?limit=6') })
    } catch (_) {}
  },

  loadVehicleInfo() {
    try {
      const info = wx.getStorageSync('vehicle_info')
      if (info) {
        this.setData({ vehicleInfo: JSON.parse(info) })
      }
    } catch (_) {}
  },

  onStartTap() {
    wx.chooseLocation({
      success: (res) => this.setData({
        startName: res.name || res.address,
        startLng: res.longitude,
        startLat: res.latitude
      })
    })
  },

  onEndTap() {
    wx.chooseLocation({
      success: (res) => this.setData({
        endName: res.name || res.address,
        endLng: res.longitude,
        endLat: res.latitude
      })
    })
  },

  onDayMinus() {
    if (this.data.totalDays > 1) {
      this.setData({ totalDays: this.data.totalDays - 1 })
    }
  },

  onDayPlus() {
    if (this.data.totalDays < 30) {
      this.setData({ totalDays: this.data.totalDays + 1 })
    }
  },

  onDifficultyTap(e) {
    this.setData({ difficulty: e.currentTarget.dataset.diff })
  },

  onTagToggle(e) {
    const tag = e.currentTarget.dataset.tag
    let tags = [...this.data.tags]
    const idx = tags.indexOf(tag)
    if (idx > -1) {
      tags.splice(idx, 1)
    } else {
      tags.push(tag)
    }
    this.setData({ tags })
  },

  onSliderChange(e) {
    this.setData({ dailyHours: e.detail.value })
  },

  onTemplateTap(e) {
    const tpl = e.currentTarget.dataset.template
    if (tpl.startName && tpl.startLng) {
      this.setData({
        startName: tpl.startName,
        startLng: tpl.startLng,
        startLat: tpl.startLat,
        totalDays: tpl.totalDays || this.data.totalDays
      })
    }
    if (tpl.tags) {
      this.setData({ tags: tpl.tags })
    }
  },

  async onGenerate() {
    if (!this.data.startName) {
      wx.showToast({ title: '请选择起点', icon: 'none' })
      return
    }
    this.setData({ generating: true })
    try {
      const startPoint = {
        name: this.data.startName,
        lng: this.data.startLng,
        lat: this.data.startLat
      }
      const endPoint = this.data.endName
        ? { name: this.data.endName, lng: this.data.endLng, lat: this.data.endLat }
        : { name: this.data.startName, lng: this.data.startLng, lat: this.data.startLat }

      const result = await post('/routes/generate', {
        totalDays: this.data.totalDays,
        startPoint,
        endPoint,
        preferences: {
          difficulty: this.data.difficulty,
          tags: this.data.tags,
          dailyDriveHours: this.data.dailyHours
        }
      })

      cacheRoute(result.routeId, result)
      wx.navigateTo({ url: '/pages/route-detail/route-detail?id=' + result.routeId })
    } catch (e) {
      wx.showToast({ title: '生成失败，请重试', icon: 'none' })
    } finally {
      this.setData({ generating: false })
    }
  }
})
