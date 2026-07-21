import { describe, it, expect, beforeEach } from 'vitest'
import { cacheRoute, getCachedRoute, isCacheStale } from '../cache'

describe('cache utils', () => {
  beforeEach(() => { localStorage.clear() })

  it('cacheRoute stores stringified data', () => {
    cacheRoute('test-1', { name: 'foo', value: 42 })
    const raw = localStorage.getItem('route_test-1')
    expect(raw).toBeTruthy()
    expect(JSON.parse(raw!)).toEqual({ name: 'foo', value: 42 })
  })

  it('cacheRoute stores timestamp', () => {
    cacheRoute('test-2', { x: 1 })
    const ts = localStorage.getItem('route_test-2_ts')
    expect(ts).toBeTruthy()
    expect(Number(ts)).toBeGreaterThan(0)
  })

  it('getCachedRoute returns parsed data', () => {
    cacheRoute('test-3', { items: [1, 2, 3] })
    const result = getCachedRoute<{ items: number[] }>('test-3')
    expect(result).toEqual({ items: [1, 2, 3] })
  })

  it('getCachedRoute returns null for missing key', () => {
    expect(getCachedRoute('nonexistent')).toBeNull()
  })

  it('isCacheStale returns true for missing key', () => {
    expect(isCacheStale('no-key')).toBe(true)
  })

  it('isCacheStale returns false for recent data', () => {
    cacheRoute('fresh', { a: 1 })
    expect(isCacheStale('fresh')).toBe(false)
  })

  it('isCacheStale returns true for expired data', () => {
    const id = 'stale-test'
    localStorage.setItem('route_' + id, JSON.stringify({ a: 1 }))
    localStorage.setItem('route_' + id + '_ts', String(Date.now() - 90000000))
    expect(isCacheStale(id)).toBe(true)
  })

  it('isCacheStale respects custom maxAge', () => {
    const id = 'custom-age'
    localStorage.setItem('route_' + id, '{}')
    localStorage.setItem('route_' + id + '_ts', String(Date.now() - 5000))
    expect(isCacheStale(id, 10000)).toBe(false)
    expect(isCacheStale(id, 2000)).toBe(true)
  })
})
