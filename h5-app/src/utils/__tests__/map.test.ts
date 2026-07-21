import { describe, it, expect } from 'vitest'
import { buildPolyline, buildMarkers, getDayColor } from '../map'
import type { Waypoint } from '../map'

function wp(lng: number, lat: number, name: string, day: number): Waypoint {
  return { lng, lat, name, day }
}

describe('map utils', () => {
  describe('buildPolyline', () => {
    it('returns empty array for less than 2 points', () => {
      expect(buildPolyline([wp(104, 30, 'A', 1)], 1)).toEqual([])
    })

    it('returns polyline for 2+ points matching day', () => {
      const wps = [wp(104, 30, 'A', 1), wp(105, 31, 'B', 1), wp(106, 32, 'C', 2)]
      const result = buildPolyline(wps, 1)
      expect(result).toHaveLength(1)
      expect(result[0].path).toHaveLength(2)
      expect(result[0].path[0]).toEqual({ longitude: 104, latitude: 30 })
    })

    it('uses dayNumber field as fallback for day', () => {
      const wps: Waypoint[] = [
        { lng: 104, lat: 30, name: 'A', dayNumber: 1 },
        { lng: 105, lat: 31, name: 'B', dayNumber: 1 },
      ]
      expect(buildPolyline(wps, 1)).toHaveLength(1)
    })

    it('returns correct stroke color based on day', () => {
      const wps = [wp(104, 30, 'A', 3), wp(105, 31, 'B', 3)]
      expect(buildPolyline(wps, 3)[0].strokeColor).toBe('#FF9800')
    })
  })

  describe('buildMarkers', () => {
    it('returns markers for matching day with correct label', () => {
      const result = buildMarkers([wp(104, 30, 'Chengdu', 1), wp(106, 32, 'Other', 2)], 1)
      expect(result).toHaveLength(1)
      expect(result[0].position).toEqual([104, 30])
      expect(result[0].label.content).toBe('Chengdu')
    })

    it('returns empty when no matching day', () => {
      expect(buildMarkers([wp(104, 30, 'A', 1)], 2)).toEqual([])
    })
  })

  describe('getDayColor', () => {
    it('returns colors for days', () => {
      expect(getDayColor(1)).toBe('#2E7D32')
      expect(getDayColor(2)).toBe('#1976D2')
      expect(getDayColor(6)).toBe('#2E7D32')
    })
  })
})
