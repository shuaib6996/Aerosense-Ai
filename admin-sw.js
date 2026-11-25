// AeroSense AI Admin Service Worker for PWA
const CACHE_NAME = 'aerosense-admin-fresh-v4';

self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => caches.delete(key))
      );
    }).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return;
  const url = new URL(event.request.url);

  // Bypass external APIs and auth
  if (url.origin.includes('googleapis.com') || url.origin.includes('firebaseio.com') || url.origin.includes('gstatic.com') || url.origin.includes('google.com')) {
    return;
  }

  // Always fetch fresh network first, never serve stale cached admin html
  event.respondWith(
    fetch(event.request, { cache: 'no-cache' })
      .then((networkResponse) => {
        return networkResponse;
      })
      .catch(() => caches.match(event.request))
  );
});
