const CACHE='roadbook-v1'
const URLS=['/app/','/app/assets/']
self.addEventListener('install',e=>{e.waitUntil(caches.open(CACHE).then(c=>c.addAll(URLS).catch(()=>{})));self.skipWaiting()})
self.addEventListener('fetch',e=>{e.respondWith(caches.match(e.request).then(r=>r||fetch(e.request).catch(()=>caches.match('/app/'))))})
