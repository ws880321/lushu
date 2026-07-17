import { get, post } from '../../utils/api'
import { cacheRoute } from '../../utils/cache'

const REGIONS = [
  { name: '成都', lng: 104.0657, lat: 30.6574, province: '四川省', city: '成都市' },
  { name: '绵阳', lng: 104.6791, lat: 31.4675, province: '四川省', city: '绵阳市' },
  { name: '乐山', lng: 103.7655, lat: 29.5521, province: '四川省', city: '乐山市' },
  { name: '康定', lng: 101.9570, lat: 30.0500, province: '四川省', city: '康定市' },
  { name: '西安', lng: 108.9402, lat: 34.3416, province: '陕西省', city: '西安市' },
  { name: '昆明', lng: 102.7123, lat: 25.0406, province: '云南省', city: '昆明市' },
  { name: '大理', lng: 100.2257, lat: 25.5895, province: '云南省', city: '大理市' },
  { name: '丽江', lng: 100.2299, lat: 26.8721, province: '云南省', city: '丽江市' },
  { name: '兰州', lng: 103.8343, lat: 36.0611, province: '甘肃省', city: '兰州市' },
  { name: '西宁', lng: 101.7782, lat: 36.6171, province: '青海省', city: '西宁市' },
  { name: '贵阳', lng: 106.7135, lat: 26.6470, province: '贵州省', city: '贵阳市' },
  { name: '凯里', lng: 107.9814, lat: 26.5669, province: '贵州省', city: '凯里市' },
  { name: '杭州', lng: 120.1536, lat: 30.2875, province: '浙江省', city: '杭州市' },
  { name: '宣城', lng: 118.7586, lat: 30.9407, province: '安徽省', city: '宣城市' },
  { name: '银川', lng: 106.2309, lat: 38.4872, province: '宁夏', city: '银川市' },
  { name: '中卫', lng: 105.1969, lat: 37.5000, province: '宁夏', city: '中卫市' },
  { name: '西双版纳', lng: 100.7974, lat: 22.0079, province: '云南省', city: '景洪市' },
  { name: '合作', lng: 102.9105, lat: 34.9838, province: '甘肃省', city: '合作市' },
  { name: '稻城', lng: 100.2981, lat: 29.0375, province: '四川省', city: '稻城县' },
  { name: '拉萨', lng: 91.1320, lat: 29.6604, province: '西藏', city: '拉萨市' },
  { name: '乌鲁木齐', lng: 87.6168, lat: 43.8256, province: '新疆', city: '乌鲁木齐市' },
  { name: '哈尔滨', lng: 126.6424, lat: 45.7567, province: '黑龙江省', city: '哈尔滨市' },
  { name: '海口', lng: 110.3312, lat: 20.0319, province: '海南省', city: '海口市' },
  { name: '厦门', lng: 118.0894, lat: 24.4798, province: '福建省', city: '厦门市' },
]

Page({
  data: {
    // Point inputs
    startText: '',
    startName: '', startLng: null, startLat: null,
    endText: '',
    endName: '', endLng: null, endLat: null,
    // Region picker
    showRegionPicker: false, pickerTarget: 'start',
    regionIndex: [0, 0],
    regionList: [
      { label: '四川省', value: '四川省', children: ['成都市', '绵阳市', '乐山市', '康定市', '稻城县'] },
      { label: '云南省', value: '云南省', children: ['昆明市', '大理市', '丽江市', '景洪市'] },
      { label: '甘肃省', value: '甘肃省', children: ['兰州市', '合作市'] },
      { label: '青海省', value: '青海省', children: ['西宁市'] },
      { label: '陕西省', value: '陕西省', children: ['西安市'] },
      { label: '贵州省', value: '贵州省', children: ['贵阳市', '凯里市'] },
      { label: '浙江省', value: '浙江省', children: ['杭州市'] },
      { label: '安徽省', value: '安徽省', children: ['宣城市'] },
      { label: '宁夏', value: '宁夏', children: ['银川市', '中卫市'] },
      { label: '西藏', value: '西藏', children: ['拉萨市'] },
      { label: '新疆', value: '新疆', children: ['乌鲁木齐市'] },
      { label: '黑龙江省', value: '黑龙江省', children: ['哈尔滨市'] },
      { label: '海南省', value: '海南省', children: ['海口市'] },
      { label: '福建省', value: '福建省', children: ['厦门市'] },
    ],
    // Preferences
    totalDays: 3,
    difficulty: 'easy',
    tags: [],
    dailyHours: 4,
    generating: false,
    popularTemplates: [],
    allTags: ['📷摄影', '⛺露营', '👶亲子', '🍜美食', '🏔️越野']
  },

  onLoad() { this.loadPopular() },

  async loadPopular() {
    try { this.setData({ popularTemplates: await get('/templates/popular?limit=6') }) } catch (_) {}
  },

  // ======== Text input ========
  onStartInput(e) { this.setData({ startText: e.detail.value || '' }) },
  onEndInput(e) { this.setData({ endText: e.detail.value || '' }) },

  // ======== Region picker ========
  openPicker(e) {
    this.setData({ showRegionPicker: true, pickerTarget: e.currentTarget.dataset.target })
  },
  onRegionChange(e) { this.setData({ regionIndex: e.detail.value }) },
  onRegionConfirm() {
    const prov = this.data.regionList[this.data.regionIndex[0]]
    const city = prov && prov.children ? prov.children[this.data.regionIndex[1]] : ''
    if (!prov || !city) return
    const region = REGIONS.find(r => r.province === prov.value && r.city === city)
    if (!region) return
    if (this.data.pickerTarget === 'start') {
      this.setData({
        startText: city, startName: city, startLng: region.lng, startLat: region.lat,
        showRegionPicker: false
      })
    } else {
      this.setData({
        endText: city, endName: city, endLng: region.lng, endLat: region.lat,
        showRegionPicker: false
      })
    }
  },
  onRegionCancel() { this.setData({ showRegionPicker: false }) },

  // ======== Text search submit (geocode by backend) ========
  onStartSubmit() {
    const name = (this.data.startText || '').trim()
    if (!name) { wx.showToast({ title: '请输入起点', icon: 'none' }); return }
    // Pass as address — backend will geocode
    this.setData({ startName: name, startLng: null, startLat: null })
    wx.showToast({ title: '已设置: ' + name, icon: 'success' })
  },
  onEndSubmit() {
    const name = (this.data.endText || '').trim()
    if (!name) { wx.showToast({ title: '请输入终点', icon: 'none' }); return }
    this.setData({ endName: name, endLng: null, endLat: null })
    wx.showToast({ title: '已设置: ' + name, icon: 'success' })
  },

  // ======== Map picker (real device fallback) ========
  onStartTap() {
    wx.chooseLocation({
      success: (res) => this.setData({
        startName: res.name || res.address, startText: res.name || res.address,
        startLng: res.longitude, startLat: res.latitude
      }),
      fail: () => this.openPicker({ currentTarget: { dataset: { target: 'start' } } })
    })
  },
  onEndTap() {
    wx.chooseLocation({
      success: (res) => this.setData({
        endName: res.name || res.address, endText: res.name || res.address,
        endLng: res.longitude, endLat: res.latitude
      }),
      fail: () => this.openPicker({ currentTarget: { dataset: { target: 'end' } } })
    })
  },

  onDayMinus() { if (this.data.totalDays > 1) this.setData({ totalDays: this.data.totalDays - 1 }) },
  onDayPlus() { if (this.data.totalDays < 30) this.setData({ totalDays: this.data.totalDays + 1 }) },
  onDifficultyTap(e) { this.setData({ difficulty: e.currentTarget.dataset.diff }) },
  onTagToggle(e) {
    const tag = e.currentTarget.dataset.tag
    let tags = [...this.data.tags]
    const idx = tags.indexOf(tag)
    idx > -1 ? tags.splice(idx, 1) : tags.push(tag)
    this.setData({ tags })
  },
  onSliderChange(e) { this.setData({ dailyHours: e.detail.value }) },

  async onGenerate() {
    if (!this.data.startName) { wx.showToast({ title: '请输入起点', icon: 'none' }); return }
    this.setData({ generating: true })
    try {
      const startPoint = {
        name: this.data.startName,
        lng: this.data.startLng, lat: this.data.startLat,
        address: this.data.startName
      }
      const endPoint = this.data.endName
        ? { name: this.data.endName, lng: this.data.endLng, lat: this.data.endLat,
            address: this.data.endName }
        : { name: this.data.startName, lng: this.data.startLng, lat: this.data.startLat,
            address: this.data.startName }

      const result = await post('/routes/generate', {
        totalDays: this.data.totalDays,
        startPoint, endPoint,
        preferences: { difficulty: this.data.difficulty, tags: this.data.tags, dailyDriveHours: this.data.dailyHours }
      })
      cacheRoute(result.routeId, result)
      wx.navigateTo({ url: '/pages/route-detail/route-detail?id=' + result.routeId })
    } catch (e) {
      wx.showToast({ title: '生成失败: ' + (e.message || '网络错误'), icon: 'none' })
    } finally {
      this.setData({ generating: false })
    }
  }
})
