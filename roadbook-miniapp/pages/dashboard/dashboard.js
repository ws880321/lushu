import { get } from '../../utils/api'

Page({
  data: {
    currentLng: null,
    currentLat: null,
    nextDistance: '--',
    nextArrival: '--',
    fuelLeft: 420,
    fuelMax: 600,
    fuelPercent: 70,
    toEndKm: 380,
    stopsNeeded: 1,
    positionMarker: [],
    nearbyAlerts: []
  },

  onShow() {
    this.getLocation()
  },

  getLocation() {
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        this.setData({
          currentLng: res.longitude,
          currentLat: res.latitude,
          positionMarker: [{
            id: 0,
            longitude: res.longitude,
            latitude: res.latitude,
            width: 24,
            height: 24
          }]
        })
        this.fetchAlerts()
      },
      fail: () => {
        wx.showToast({ title: '获取位置失败', icon: 'none' })
      }
    })
  },

  async fetchAlerts() {
    if (!this.data.currentLng) return
    try {
      const data = await get('/nearby/alerts', {
        lng: this.data.currentLng,
        lat: this.data.currentLat,
        radius: 50000,
        vehicleRangeLeft: this.data.fuelLeft
      })
      this.setData({ nearbyAlerts: data.alerts || [] })
    } catch (_) {
      // Silently ignore fetch errors
    }
  },

  onAlertTap(e) {
    const item = e.detail
    if (item && item.poiLng && item.poiLat) {
      wx.openLocation({
        longitude: item.poiLng,
        latitude: item.poiLat,
        name: item.poiName
      })
    }
  }
})
