/**
 * Service Worker pour cacher les images ImgBB
 * Permet l'accès offline aux photos déjà chargées
 */

const CACHE_NAME = 'imgbb-images-v1';
const IMGBB_DOMAIN = 'i.imgbb.com';
const CACHE_MAX_SIZE = 52428800; // 50MB en bytes

/**
 * Intercepte les requêtes ImgBB et les cache
 */
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // Ne cacher que les images ImgBB
  if (!url.hostname.includes(IMGBB_DOMAIN)) {
    return;
  }

  // Ne cacher que les GET
  if (request.method !== 'GET') {
    return;
  }

  event.respondWith(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.match(request).then((response) => {
        // Si en cache, retourner du cache
        if (response) {
          console.log(`[SW] Cache hit: ${url.pathname}`);
          return response;
        }

        // Sinon, fetch et cacher
        console.log(`[SW] Fetching and caching: ${url.pathname}`);
        return fetch(request)
          .then((fetchResponse) => {
            // Vérifier que la réponse est valide
            if (!fetchResponse || fetchResponse.status !== 200) {
              return fetchResponse;
            }

            // Cloner la réponse pour la cacher
            const responseToCache = fetchResponse.clone();

            // Cacher de manière asynchrone
            cache
              .put(request, responseToCache)
              .then(() => {
                // Nettoyer le cache si trop volumineux
                cleanupCache(cache);
              })
              .catch((err) => {
                console.warn('[SW] Cache put failed:', err);
              });

            return fetchResponse;
          })
          .catch((fetchError) => {
            // Si fetch échoue, retourner depuis cache (même périmé)
            console.warn(`[SW] Fetch failed for ${url.pathname}, trying stale cache`);
            return cache.match(request).then((staleResponse) => {
              if (staleResponse) {
                console.log(`[SW] Using stale cache: ${url.pathname}`);
                return staleResponse;
              }

              // Aucun cache disponible, retourner erreur
              console.error(`[SW] No cache available for ${url.pathname}`);
              throw fetchError;
            });
          });
      });
    })
  );
});

/**
 * Nettoie le cache si la taille dépasse la limite
 */
async function cleanupCache(cache) {
  try {
    const cacheStorage = await caches.open(CACHE_NAME);
    const keys = await cacheStorage.keys();

    if (keys.length === 0) return;

    // Calculer la taille totale du cache
    let totalSize = 0;
    const responses = await Promise.all(
      keys.map((request) => cacheStorage.match(request))
    );

    for (const response of responses) {
      if (response && response.blob) {
        const blob = await response.blob();
        totalSize += blob.size;
      }
    }

    // Si dépasse la limite, supprimer les plus vieilles entrées
    if (totalSize > CACHE_MAX_SIZE) {
      console.log(
        `[SW] Cache size (${(totalSize / 1024 / 1024).toFixed(2)}MB) exceeds limit, cleaning up...`
      );

      // Supprimer les entrées jusqu'à être en dessous de 70% de la limite
      const targetSize = CACHE_MAX_SIZE * 0.7;
      for (const key of keys) {
        if (totalSize <= targetSize) break;

        const response = await cacheStorage.match(key);
        if (response && response.blob) {
          const blob = await response.blob();
          totalSize -= blob.size;
          await cacheStorage.delete(key);
          console.log(`[SW] Deleted from cache: ${key.url}`);
        }
      }
    }
  } catch (err) {
    console.warn('[SW] Cache cleanup error:', err);
  }
}

/**
 * Handle activation: supprimer les anciens caches
 */
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          // Supprimer les caches qui ne sont pas la version actuelle
          if (cacheName !== CACHE_NAME && cacheName.startsWith('imgbb-images')) {
            console.log(`[SW] Deleting old cache: ${cacheName}`);
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
});

/**
 * Handle installation
 */
self.addEventListener('install', (event) => {
  console.log('[SW] Service Worker installed');
  self.skipWaiting(); // Activer immédiatement
});
