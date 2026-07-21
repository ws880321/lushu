import { describe, it, expect } from 'vitest'
import { REGIONS, getProvinceList, getCityList } from '../regions'

describe('regions', () => {
  it('has expected provinces', () => {
    const provinces = Object.keys(REGIONS)
    expect(provinces).toContain('四川')
    expect(provinces).toContain('云南')
    expect(provinces).toContain('西藏')
  })

  it('each province has valid city coordinates', () => {
    for (const [, data] of Object.entries(REGIONS)) {
      const cities = Object.keys(data.cities)
      expect(cities.length).toBeGreaterThan(0)
      for (const [, coords] of Object.entries(data.cities)) {
        expect(coords).toHaveLength(2)
        expect(typeof coords[0]).toBe('number')
        expect(typeof coords[1]).toBe('number')
      }
    }
  })

  it('getProvinceList returns same keys as REGIONS', () => {
    expect(getProvinceList().sort()).toEqual(Object.keys(REGIONS).sort())
  })

  it('getCityList returns cities for valid province', () => {
    const cities = getCityList('四川')
    expect(cities).toHaveProperty('成都')
    expect(cities['成都']).toEqual([104.0657, 30.6574])
  })

  it('getCityList returns empty object for invalid province', () => {
    expect(getCityList('火星')).toEqual({})
  })
})
