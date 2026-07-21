const DAY_COLORS = ['#2E7D32', '#1976D2', '#FF9800', '#9C27B0', '#E53935']

export interface Waypoint {
  lng: number
  lat: number
  dayNumber?: number
  day?: number
  name: string
}

export function buildPolyline(waypoints: Waypoint[], day: number): Array<{
  path: Array<{ longitude: number, latitude: number }>
  strokeColor: string
  strokeWeight: number
  strokeOpacity: number
}> {
  const pts = waypoints.filter(w => (w.dayNumber || w.day) === day).map(w => ({
    longitude: w.lng,
    latitude: w.lat,
  }))
  if (pts.length < 2) return []
  return [{
    path: pts,
    strokeColor: DAY_COLORS[(day - 1) % 5],
    strokeWeight: 6,
    strokeOpacity: 0.8,
  }]
}

export function buildMarkers(waypoints: Waypoint[], day: number): Array<{
  id: number
  position: [number, number]
  label: { content: string; fontSize: number; direction: string }
}> {
  return waypoints.filter(w => (w.dayNumber || w.day) === day).map((w, i) => ({
    id: i,
    position: [w.lng, w.lat] as [number, number],
    label: { content: w.name, fontSize: 12, direction: 'top' },
  }))
}

export function getDayColor(day: number): string {
  return DAY_COLORS[(day - 1) % 5]
}
