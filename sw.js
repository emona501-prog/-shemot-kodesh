const CACHE = 'shemot-v8';
self.addEventListener('install', e => self.skipWaiting());
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(keys =>
    Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))
  ));
});
self.addEventListener('fetch', e => {
  e.respondWith(fetch(e.request).then(r => {
    const rc = r.clone();
    caches.open(CACHE).then(c => c.put(e.request, rc));
    return r;
  }).catch(() => caches.match(e.request)));
});
