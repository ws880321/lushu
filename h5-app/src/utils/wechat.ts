const WX_SCRIPT = 'https://res.wx.qq.com/open/js/jweixin-1.6.0.js'
let wxReady = false

declare const wx: any

export function isWeChat(): boolean {
  return /MicroMessenger/i.test(navigator.userAgent || '')
}

function loadScript(src: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const s = document.createElement('script')
    s.src = src; s.onload = () => resolve(); s.onerror = () => reject(new Error('script load failed'))
    document.head.appendChild(s)
  })
}

async function getSignature(url: string): Promise<any> {
  const token = localStorage.getItem('app_token') || ''
  const resp = await fetch(`/api/v1/auth/js-sdk-sign?url=${encodeURIComponent(url)}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  const json = await resp.json()
  if (json.code !== 0) throw new Error(json.message || '签名失败')
  return json.data
}

export async function initWxJsSdk(): Promise<boolean> {
  if (!isWeChat()) return false
  if (wxReady) return true
  try { await loadScript(WX_SCRIPT) } catch { return false }
  try {
    const sig = await getSignature(window.location.href.split('#')[0])
    return new Promise((resolve) => {
      wx.config({ debug: false, appId: sig.appId, timestamp: sig.timestamp, nonceStr: sig.nonceStr, signature: sig.signature, jsApiList: ['getLocation'] })
      wx.ready(() => { wxReady = true; resolve(true) })
      wx.error(() => resolve(false))
    })
  } catch { return false }
}

export async function wxGetLocation(): Promise<{ lng: number; lat: number } | null> {
  if (!wxReady) return null
  return new Promise((resolve) => {
    wx.getLocation({
      type: 'gcj02',
      success: (res: any) => resolve({ lng: res.longitude, lat: res.latitude }),
      fail: () => resolve(null),
      cancel: () => resolve(null),
    })
  })
}
