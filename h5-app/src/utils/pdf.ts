export function exportRoutePDF(route: any): void {
  const w = window.open('', '_blank', 'width=800,height=600')
  if (!w) return
  const days = route.itinerary || []
  let html = `<!doctype html><html><head><meta charset="utf-8"><title>${route.title || '路书'}</title>
<style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:'PingFang SC','Microsoft YaHei',sans-serif;max-width:800px;margin:0 auto;padding:20px}
h1{font-size:24px;text-align:center;color:#2E7D32;margin-bottom:8px}.sum{text-align:center;color:#666;font-size:14px;margin-bottom:20px;padding-bottom:12px;border-bottom:2px solid #2E7D32}
.day{margin:16px 0;page-break-inside:avoid}.day-h{font-size:16px;font-weight:700;color:#2E7D32;padding:8px 0;border-bottom:1px solid #eee}.day-s{font-size:12px;color:#999;margin:4px 0 8px;font-style:italic}
.wp{display:flex;gap:10px;padding:8px 0;border-bottom:1px dotted #f0f0f0}.wp-l{width:20px;text-align:center}.wp-d{width:10px;height:10px;border-radius:50%;background:#2E7D32;margin:5px auto 0}.wp-d.s{background:#4CAF50}.wp-d.r{background:#FF9800}.wp-d.h{background:#9C27B0}.wp-d.g{background:#E53935}.wp-body{flex:1}.wp-n{font-size:14px;font-weight:600}.wp-t{font-size:11px;color:#666;margin:2px 0}.wp-tip{font-size:12px;color:#888;background:#f9f9f9;padding:4px 8px;border-radius:4px;margin-top:4px}
.footer{text-align:center;color:#999;font-size:11px;padding:20px 0;border-top:1px solid #eee;margin-top:20px}
@media print{body{padding:10px}}
</style></head><body>
<h1>${route.title || '自驾路书'}</h1>
<div class="sum">${route.totalDays || 0}天 · ${route.totalDistanceKm || 0}km · 预估 ¥${route.estimatedCost?.totalYuan || 0}</div>`

  for (const d of days) {
    html += `<div class="day"><div class="day-h">Day ${d.day} — ${d.distanceKm || 0}km / ${d.driveTimeMin || 0}分钟</div>`
    if (d.summary) html += `<div class="day-s">${d.summary}</div>`
    for (const wp of (d.waypoints || [])) {
      const tChar = (wp.type || '').substring(0, 1)
      html += `<div class="wp"><div class="wp-l"><div class="wp-d ${tChar}"></div></div><div class="wp-body"><div class="wp-n">${wp.name}</div><div class="wp-t">${wp.arrival || ''} <span style="background:#e8f5e9;padding:1px 4px;border-radius:3px">${wp.type || ''}</span></div>`
      if (wp.description || wp.tips) html += `<div class="wp-tip">${wp.description || wp.tips}</div>`
      html += `</div></div>`
    }
    html += '</div>'
  }
  html += `<div class="footer">由 路书 生成 · www.wychen.net</div></body></html>`
  w.document.write(html); w.document.close()
  setTimeout(() => w.print(), 500)
}
