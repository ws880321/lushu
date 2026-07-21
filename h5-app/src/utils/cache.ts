const CACHE_PREFIX = 'route_'
const TS_SUFFIX = '_ts'
const MAX_AGE_MS = 86400000

export function cacheRoute(id: string | number, data: unknown): void {
  localStorage.setItem(CACHE_PREFIX + id, JSON.stringify(data))
  localStorage.setItem(CACHE_PREFIX + id + TS_SUFFIX, Date.now().toString())
}

export function getCachedRoute<T = unknown>(id: string | number): T | null {
  const raw = localStorage.getItem(CACHE_PREFIX + id)
  return raw ? JSON.parse(raw) as T : null
}

export function isCacheStale(id: string | number, maxAgeMs = MAX_AGE_MS): boolean {
  const ts = localStorage.getItem(CACHE_PREFIX + id + TS_SUFFIX)
  if (!ts) return true
  return Date.now() - parseInt(ts) > maxAgeMs
}
