const TYPES: Record<string, string> = {
  scenic: '景点', restaurant: '餐厅', hotel: '住宿', gas: '加油', rest: '休息',
  start: '出发', end: '终点', break: '休息点', food: '美食', photo: '拍照',
  custom: '自定义', service: '服务区',
}

function typeLabel(t: string): string { return TYPES[t] || t || '途经点' }

function wrapText(ctx: CanvasRenderingContext2D, text: string, cx: number, y: number, maxW: number, lineH: number): number {
  ctx.textAlign = 'center'
  const chars = [...text]
  let line = ''
  for (const ch of chars) {
    const test = line + ch
    if (ctx.measureText(test).width > maxW && line.length > 0) {
      ctx.fillText(line, cx, y)
      y += lineH; line = ch
    } else { line = test }
  }
  if (line) ctx.fillText(line, cx, y)
  return y + lineH
}

function roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number): void {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.lineTo(x + w - r, y); ctx.quadraticCurveTo(x + w, y, x + w, y + r)
  ctx.lineTo(x + w, y + h - r); ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
  ctx.lineTo(x + r, y + h); ctx.quadraticCurveTo(x, y + h, x, y + h - r)
  ctx.lineTo(x, y + r); ctx.quadraticCurveTo(x, y, x + r, y)
  ctx.closePath()
}

export async function generateRoutePoster(route: any): Promise<string | null> {
  if (!route) return null

  const W = 750, H = 1334
  const canvas = document.createElement('canvas')
  canvas.width = W; canvas.height = H
  const ctx = canvas.getContext('2d')
  if (!ctx) return null

  const bg = ctx.createLinearGradient(0, 0, 0, H)
  bg.addColorStop(0, '#1B5E20')
  bg.addColorStop(0.35, '#2E7D32')
  bg.addColorStop(0.35, '#f5f5f5')
  bg.addColorStop(1, '#ffffff')
  ctx.fillStyle = bg; ctx.fillRect(0, 0, W, H)

  ctx.fillStyle = '#fff'
  ctx.font = '24px sans-serif'; ctx.textAlign = 'center'
  ctx.fillText('🗺️ 路书', W / 2, 80)

  ctx.font = 'bold 42px sans-serif'
  wrapText(ctx, route.title || '自驾路线', W / 2, 150, 600, 50)

  ctx.font = '26px sans-serif'; ctx.fillStyle = 'rgba(255,255,255,0.85)'
  ctx.fillText(`${route.totalDays || 0} 天 · ${route.totalDistanceKm || 0} km · ${route.startPoint || '?'} → ${route.endPoint || '?'}`, W / 2, 240)

  ctx.fillStyle = 'rgba(255,255,255,0.3)'
  ctx.fillRect(100, 275, 550, 1)

  const cardY = 310, cardW = 170, cardH = 100, gap = 20
  const cards = [
    { label: '总天数', value: `${route.totalDays || 0}天` },
    { label: '总里程', value: `${route.totalDistanceKm || 0}km` },
    { label: '预计费用', value: `¥${route.estimatedCost?.totalYuan || 0}` },
  ]
  const startX = (W - (cards.length * cardW + (cards.length - 1) * gap)) / 2

  cards.forEach((c, i) => {
    const x = startX + i * (cardW + gap)
    ctx.fillStyle = '#fff'; roundRect(ctx, x, cardY, cardW, cardH, 12); ctx.fill()
    ctx.fillStyle = '#2E7D32'; ctx.font = 'bold 36px sans-serif'; ctx.textAlign = 'center'
    ctx.fillText(c.value, x + cardW / 2, cardY + 48)
    ctx.fillStyle = '#999'; ctx.font = '22px sans-serif'
    ctx.fillText(c.label, x + cardW / 2, cardY + 80)
  })

  let itemY = 460
  const itinerary = route.itinerary || []
  const shown = Array.isArray(itinerary) ? itinerary.slice(0, 5) : Object.entries(itinerary).slice(0, 5)

  ctx.textAlign = 'left'
  for (const entry of shown) {
    const [day, dayData] = Array.isArray(entry) ? entry : [String((shown.indexOf(entry) as number) + 1), entry]
    const wps: any[] = (dayData as any)?.waypoints || []
    if (itemY > H - 300) break

    ctx.fillStyle = '#333'; ctx.font = 'bold 28px sans-serif'
    const label = Array.isArray(entry) ? `Day ${day}` : `Day ${shown.indexOf(entry) + 1}`
    ctx.fillText(`${label}  ·  ${wps.length} 个站点`, 40, itemY)
    itemY += 40

    const showWps = wps.slice(0, 6)
    for (const wp of showWps) {
      ctx.fillStyle = '#2E7D32'
      ctx.beginPath(); ctx.arc(60, itemY - 6, 8, 0, Math.PI * 2); ctx.fill()
      ctx.fillStyle = '#333'; ctx.font = '24px sans-serif'
      ctx.fillText(`${wp.name || '?'}`, 85, itemY)
      if (wp.type) {
        ctx.fillStyle = '#999'; ctx.font = '20px sans-serif'
        ctx.fillText(typeLabel(wp.type), 85, itemY + 26)
        itemY += 30
      }
      itemY += 32
    }
    itemY += 10

    if (itemY < H - 300 && entry !== shown[shown.length - 1]) {
      ctx.strokeStyle = '#e0e0e0'; ctx.lineWidth = 1
      ctx.beginPath(); ctx.moveTo(40, itemY); ctx.lineTo(W - 40, itemY); ctx.stroke()
      itemY += 16
    }
  }

  const footerY = H - 160
  ctx.fillStyle = '#1B5E20'
  ctx.fillRect(0, footerY - 40, W, H - footerY + 40)

  ctx.fillStyle = 'rgba(255,255,255,0.8)'; ctx.font = '22px sans-serif'; ctx.textAlign = 'center'
  ctx.fillText('扫码体验 路书 App', W / 2, footerY + 20)
  ctx.fillStyle = 'rgba(255,255,255,0.5)'; ctx.font = '18px sans-serif'
  ctx.fillText('www.wychen.net/app/', W / 2, footerY + 52)

  const qrX = W / 2 - 50, qrY = footerY + 70, qrS = 100
  ctx.fillStyle = '#fff'; ctx.fillRect(qrX, qrY, qrS, qrS)
  ctx.fillStyle = '#1B5E20'
  for (let r = 0; r < 5; r++) {
    for (let c = 0; c < 5; c++) {
      if ((r + c) % 2 === 0 && !(r === 2 && c === 2)) {
        ctx.fillRect(qrX + c * 20, qrY + r * 20, 20, 20)
      }
    }
  }

  return canvas.toDataURL('image/jpeg', 0.9)
}
