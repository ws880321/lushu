# Tasks 13+14 Report: Mini-Program Init & Login Page

## Status: Complete

### Task 13 — Project Initialization
Created `roadbook-miniapp/` with all required files:

| File | Purpose |
|------|---------|
| `project.config.json` | WeChat DevTools config, appid placeholder, lib 3.3.0 |
| `app.json` | 4 page routes, green nav bar, location permission |
| `app.js` | Global state (baseUrl, token, userInfo), token restore on launch |
| `app.wxss` | Base styles (page bg, .card, .card-title) |
| `utils/api.js` | `get()`/`post()` with JWT header, 401 auto-redirect |
| `utils/cache.js` | `cacheRoute()`, `getCachedRoute()`, `isCacheStale()` (24h TTL) |
| `utils/map.js` | `buildPolyline()`, `buildMarkers()` with 5-day color cycle |
| `pages/generate/generate` | Stub page directory (registered in app.json) |
| `pages/route-detail/route-detail` | Stub page directory |
| `pages/dashboard/dashboard` | Stub page directory |

### Task 14 — Login Page
Created `pages/login/` with:

| File | Lines | Purpose |
|------|-------|---------|
| `login.wxml` | 14 | Centered layout with logo, WeChat button, privacy tip |
| `login.js` | 20 | `handleLogin()`: wx.login -> POST /auth/wechat-login -> store token -> navigate |
| `login.wxss` | 46 | Green centered design, full-height flex, pill button |
| `login.json` | 4 | Nav title "登录", no custom components |

### Concerns

1. **`appid` placeholder** in `project.config.json` — replace `"your-appid-here"` with the real WeChat appid before opening in DevTools.
2. **`baseUrl` placeholder** in `app.js` — replace `"https://your-domain.com/api/v1"` with the actual backend URL.
3. **`wx.switchTab`** is used on login success, but the target pages (`generate`, `route-detail`, `dashboard`) are registered as regular pages in `app.json`, not tab pages. `switchTab` requires a corresponding `tabBar` config in `app.json`. Either:
   - Add a `tabBar` to `app.json` listing the 3 target pages, or
   - Change the redirect to `wx.reLaunch()` instead.
4. **`open-type="getUserInfo"`** on the login button is a WeChat legacy approach. For the current getPhoneNumber/login flow, the button primarily triggers the `bindtap` handler; the `open-type` is redundant but harmless.
5. **No .gitignore** — add one to exclude `node_modules/`, `miniprogram_npm/`, and local config overrides (`project.private.config.json`).
