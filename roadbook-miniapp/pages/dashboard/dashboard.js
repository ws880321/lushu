import { get } from '../../utils/api'

Page({
  data: {
    currentLng: null,
    currentLat: null,
    nextDistance: '--',
    nextArrival: '--',
    fuelLeft: 0,
    fuelMax: 600,
    fuelPercent: 0,
    toEndKm: 0,
    stopsNeeded: 0,
    positionMarker: [],
    nearbyAlerts: []
  },

  onShow() {
    this.getLocation()
    this.calcFuel()
  },

  async calcFuel() {
    let rangeFull = 420
    let routeDistance = 0
    try {
      const vehicles = await get('/vehicles')
      if (vehicles && vehicles.length && vehicles[0].rangeFull) rangeFull = vehicles[0].rangeFull
    } catch {}
    try {
      const data = await get('/routes', { size: 1 })
      const routes = data.content || data || []
      if (routes.length && routes[0].totalDistance) routeDistance = parseFloat(routes[0].totalDistance) || 0
    } catch {}
    const fuelLeft = Math.max(0, rangeFull - routeDistance)
    const fuelPercent = rangeFull > 0 ? Math.round((fuelLeft / rangeFull) * 100) : 100
    const stopsNeeded = rangeFull > 0 && routeDistance > rangeFull ? Math.floor(routeDistance / rangeFull) : 0
    this.setData({ fuelLeft, fuelMax: rangeFull, fuelPercent, toEndKm: routeDistance, stopsNeeded })
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
