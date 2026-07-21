const AMAP_REST_KEY = 'df1e5cca15cf847eeae3e94060be2525'

/** JSONP — bypasses browser CORS for Amap REST API */
export function amapJsonp(url: string): Promise<any> {
  return new Promise((resolve, reject) => {
    const cb = '_amap_' + Date.now() + '_' + Math.random().toString(36).slice(2)
    ;(window as any)[cb] = (data: any) => { delete (window as any)[cb]; remove(); resolve(data) }
    const s = document.createElement('script')
    s.src = url + '&callback=' + cb
    s.onerror = () => { delete (window as any)[cb]; remove(); reject(new Error('jsonp')) }
    function remove() { if (s.parentNode) s.parentNode.removeChild(s) }
    s && document.head.appendChild(s)
  })
}

/** Reverse geocode lng,lat to address string */
export function reverseGeocode(lng: number, lat: number): Promise<string> {
  const u = `https://restapi.amap.com/v3/geocode/regeo?key=${AMAP_REST_KEY}&location=${lng},${lat}&output=JSON&extensions=base`
  return amapJsonp(u).then(j =>
    j.status === '1' && j.regeocode?.formatted_address ? j.regeocode.formatted_address : lng.toFixed(4) + ',' + lat.toFixed(4)
  ).catch(() => lng.toFixed(4) + ',' + lat.toFixed(4))
}
