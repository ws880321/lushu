import { get } from '../../utils/api'
import { getCachedRoute, cacheRoute, isCacheStale } from '../../utils/cache'
import { buildPolyline, buildMarkers } from '../../utils/map'

Page({
  data: {
    route: null,
    activeDay: 1,
    currentDay: null,
    polylines: [],
    markers: [],
    centerLng: 104,
    centerLat: 30
  },

  onLoad(options) {
    if (options.id) {
      this.loadRoute(options.id)
    } else {
      wx.showToast({ title: '缺少路线ID', icon: 'none' })
    }
  },

  async loadRoute(id) {
    let route = getCachedRoute(id)
    let fromNet = false

    if (!route || isCacheStale(id)) {
      try {
        route = await get('/routes/' + id)
        cacheRoute(id, route)
        fromNet = true
      } catch (e) {
        // keep cached version if available
      }
    }

    if (!route) {
      wx.showToast({ title: '加载失败', icon: 'none' })
      return
    }

    this.setData({ route })

    if (!fromNet) {
      wx.showToast({ title: '当前离线，显示缓存', icon: 'none', duration: 2000 })
    }

    this.selectDay(1)
  },

  selectDay(day) {
    const cd = this.data.route.itinerary.find(d => d.day === day)
    if (!cd) return

    this.setData({
      activeDay: day,
      currentDay: cd,
      polylines: [buildPolyline(cd.waypoints, day)],
      markers: buildMarkers(cd.waypoints, day),
      centerLng: cd.waypoints[0]?.lng || 104,
      centerLat: cd.waypoints[0]?.lat || 30
    })
  },

  onDayTab(e) {
    const day = parseInt(e.currentTarget.dataset.day, 10)
    this.selectDay(day)
  },

  onMarkerTap(e) {
    const wp = this.data.currentDay?.waypoints[e.markerId]
    if (!wp) return

    wx.showModal({
      title: wp.name,
      content: (wp.tips || '') + '\n评分: ' + (wp.driveScore || '暂无'),
      confirmText: '导航',
      success: (r) => {
        if (r.confirm) {
          wx.openLocation({
            longitude: wp.lng,
            latitude: wp.lat,
            name: wp.name,
            scale: 16
          })
        }
      }
    })
  },

  onWaypointTap(e) {
    const index = e.currentTarget.dataset.index
    const wp = this.data.currentDay?.waypoints[index]
    if (!wp) return

    wx.showModal({
      title: wp.name,
      content: (wp.tips || '') + '\n评分: ' + (wp.driveScore || '暂无'),
      confirmText: '导航',
      success: (r) => {
        if (r.confirm) {
          wx.openLocation({
            longitude: wp.lng,
            latitude: wp.lat,
            name: wp.name,
            scale: 16
          })
        }
      }
    })
  }
})
