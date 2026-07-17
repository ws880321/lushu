export function cacheRoute(id, data) {
  wx.setStorageSync(`route_${id}`, JSON.stringify(data))
  wx.setStorageSync(`route_${id}_ts`, Date.now())
}

export function getCachedRoute(id) {
  const raw = wx.getStorageSync(`route_${id}`)
  return raw ? JSON.parse(raw) : null
}

export function isCacheStale(id, maxAgeMs = 86400000) {
  const ts = wx.getStorageSync(`route_${id}_ts`)
  return !ts || (Date.now() - ts > maxAgeMs)
}
