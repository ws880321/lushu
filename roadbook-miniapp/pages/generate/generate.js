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
    allTags: ['📷摄影', '⛺露营', '👶亲子', '🍜美食', '🏔️越野'],
    showStartInput: false,
    showEndInput: false,
    startCoordInput: '',
    endCoordInput: '',
    // 快捷城市预设
    quickCities: [
      { name: '成都', lng: 104.0657, lat: 30.6574 },
      { name: '西安', lng: 108.9402, lat: 34.3416 },
      { name: '昆明', lng: 102.7123, lat: 25.0406 },
      { name: '兰州', lng: 103.8343, lat: 36.0611 },
      { name: '贵阳', lng: 106.7135, lat: 26.6470 },
      { name: '杭州', lng: 120.1536, lat: 30.2875 },
    ]
  },

  onLoad() {
    this.loadPopular()
    this.loadVehicleInfo()
  },

  async loadPopular() {
    try {
      const data = await get('/templates/popular?limit=6')
      this.setData({ popularTemplates: data })
    } catch (_) {}
  },

  loadVehicleInfo() {
    try {
      const info = wx.getStorageSync('vehicle_info')
      if (info) this.setData({ vehicleInfo: JSON.parse(info) })
    } catch (_) {}
  },

  onStartTap() {
    wx.chooseLocation({
      success: (res) => {
        this.setData({
          startName: res.name || res.address,
          startLng: res.longitude,
          startLat: res.latitude
        })
      },
      fail: () => {
        this.setData({ showStartInput: true })
      }
    })
  },

  onEndTap() {
    wx.chooseLocation({
      success: (res) => {
        this.setData({
          endName: res.name || res.address,
          endLng: res.longitude,
          endLat: res.latitude
        })
      },
      fail: () => {
        this.setData({ showEndInput: true })
      }
    })
  },

  onStartCoordSubmit() {
    const parts = this.data.startCoordInput.split(',')
    if (parts.length === 2) {
      const lng = parseFloat(parts[0].trim())
      const lat = parseFloat(parts[1].trim())
      if (!isNaN(lng) && !isNaN(lat)) {
        this.setData({
          startName: '手动坐标 (' + lng.toFixed(4) + ',' + lat.toFixed(4) + ')',
          startLng: lng, startLat: lat,
          showStartInput: false, startCoordInput: ''
        })
        return
      }
    }
    wx.showToast({ title: '格式: 经度,纬度  如 104.0657,30.6574', icon: 'none' })
  },

  onEndCoordSubmit() {
    const parts = this.data.endCoordInput.split(',')
    if (parts.length === 2) {
      const lng = parseFloat(parts[0].trim())
      const lat = parseFloat(parts[1].trim())
      if (!isNaN(lng) && !isNaN(lat)) {
        this.setData({
          endName: '手动坐标 (' + lng.toFixed(4) + ',' + lat.toFixed(4) + ')',
          endLng: lng, endLat: lat,
          showEndInput: false, endCoordInput: ''
        })
        return
      }
    }
    wx.showToast({ title: '格式: 经度,纬度  如 104.0657,30.6574', icon: 'none' })
  },

  onQuickLoc(e) {
    const i = e.currentTarget.dataset.index
    const loc = this.data.quickCities[i]
    this.setData({
      startName: loc.name,
      startLng: loc.lng,
      startLat: loc.lat,
      showStartInput: false
    })
  },

  onDayMinus() {
    if (this.data.totalDays > 1) this.setData({ totalDays: this.data.totalDays - 1 })
  },
  onDayPlus() {
    if (this.data.totalDays < 30) this.setData({ totalDays: this.data.totalDays + 1 })
  },
  onDifficultyTap(e) {
    this.setData({ difficulty: e.currentTarget.dataset.diff })
  },
  onTagToggle(e) {
    const tag = e.currentTarget.dataset.tag
    let tags = [...this.data.tags]
    const idx = tags.indexOf(tag)
    idx > -1 ? tags.splice(idx, 1) : tags.push(tag)
    this.setData({ tags })
  },
  onSliderChange(e) {
    this.setData({ dailyHours: e.detail.value })
  },

  onTemplateTap(e) {
    const tpl = e.currentTarget.dataset.template
    if (tpl.name) {
      wx.showToast({ title: '已选择: ' + tpl.name, icon: 'none' })
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
      console.error('生成失败', e)
      wx.showToast({ title: '生成失败: ' + (e.message || '网络错误'), icon: 'none' })
    } finally {
      this.setData({ generating: false })
    }
  }
})
