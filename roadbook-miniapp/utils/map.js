const DAY_COLORS = ['#2E7D32', '#1565C0', '#E65100', '#6A1B9A', '#C62828']

export function buildPolyline(waypoints, day) {
  const points = waypoints
    .filter(w => w.dayNumber === day)
    .map(w => ({ longitude: w.lng, latitude: w.lat }))
  return {
    points,
    color: DAY_COLORS[(day - 1) % 5],
    width: 4,
    dottedLine: false
  }
}

export function buildMarkers(waypoints, day) {
  return waypoints
    .filter(w => w.dayNumber === day)
    .map((w, i) => ({
      id: i,
      longitude: w.lng,
      latitude: w.lat,
      width: 30,
      height: 30,
      callout: {
        content: w.name,
        fontSize: 12,
        borderRadius: 4,
        padding: 4,
        display: 'ALWAYS'
      }
    }))
}
