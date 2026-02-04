/**
 * Utilitaire pour charger les photos de manière sûre
 * Support du mode offline via Service Worker cache
 */

import React from 'react';

/**
 * Charge une image ImgBB de manière sûre
 * - Online: charge depuis ImgBB et cache via Service Worker
 * - Offline: retourne depuis cache si disponible
 *
 * @param {string} imgbbUrl - URL ImgBB
 * @returns {Promise<string|null>} - URL de l'image ou null si pas disponible
 */
export const loadPhotoSafely = async (imgbbUrl) => {
  if (!imgbbUrl) {
    return null;
  }

  // Si online, retourner l'URL directement
  // Le Service Worker se charge du caching
  if (navigator.onLine) {
    return imgbbUrl;
  }

  // Mode offline: essayer de charger depuis le cache
  try {
    if ('caches' in window) {
      const cache = await caches.open('imgbb-images-v1');
      const response = await cache.match(imgbbUrl);

      if (response) {
        console.log('[Photo] Loaded from cache (offline):', imgbbUrl);
        return imgbbUrl; // L'URL reste la même, mais Service Worker retournera du cache
      }
    }
  } catch (error) {
    console.warn('[Photo] Cache access failed:', error);
  }

  // Pas en cache et offline
  console.warn('[Photo] Not available offline:', imgbbUrl);
  return null;
};

/**
 * Vérifie si une photo est disponible en cache
 * @param {string} imgbbUrl - URL ImgBB
 * @returns {Promise<boolean>}
 */
export const isPhotoCached = async (imgbbUrl) => {
  if (!imgbbUrl || !('caches' in window)) {
    return false;
  }

  try {
    const cache = await caches.open('imgbb-images-v1');
    const response = await cache.match(imgbbUrl);
    return !!response;
  } catch (error) {
    console.warn('[Photo] Cache check failed:', error);
    return false;
  }
};

/**
 * Pré-cache une photo (utile pour les photos critiques)
 * @param {string} imgbbUrl - URL ImgBB
 */
export const preCachePhoto = async (imgbbUrl) => {
  if (!imgbbUrl || !('caches' in window) || !navigator.onLine) {
    return;
  }

  try {
    const cache = await caches.open('imgbb-images-v1');
    const response = await fetch(imgbbUrl);

    if (response.ok) {
      await cache.put(imgbbUrl, response);
      console.log('[Photo] Pre-cached:', imgbbUrl);
    }
  } catch (error) {
    console.warn('[Photo] Pre-cache failed:', error);
  }
};

/**
 * Vide le cache des images
 */
export const clearPhotoCache = async () => {
  try {
    if ('caches' in window) {
      await caches.delete('imgbb-images-v1');
      console.log('[Photo] Cache cleared');
    }
  } catch (error) {
    console.warn('[Photo] Cache clear failed:', error);
  }
};

/**
 * Obtient les stats du cache
 * @returns {Promise<Object>} - {count, size}
 */
export const getPhotoCacheStats = async () => {
  if (!('caches' in window)) {
    return { count: 0, size: 0 };
  }

  try {
    const cache = await caches.open('imgbb-images-v1');
    const keys = await cache.keys();

    let totalSize = 0;
    for (const request of keys) {
      const response = await cache.match(request);
      if (response) {
        const blob = await response.blob();
        totalSize += blob.size;
      }
    }

    return {
      count: keys.length,
      size: totalSize,
      sizeInMB: (totalSize / 1024 / 1024).toFixed(2),
    };
  } catch (error) {
    console.warn('[Photo] Cache stats failed:', error);
    return { count: 0, size: 0 };
  }
};

/**
 * Hook React pour charger une photo de manière sûre
 * Utile dans les composants
 */
export const useOfflinePhoto = (imgbbUrl) => {
  const [photoUrl, setPhotoUrl] = React.useState(imgbbUrl);
  const [loading, setLoading] = React.useState(false);
  const [cached, setCached] = React.useState(false);

  React.useEffect(() => {
    const loadPhoto = async () => {
      setLoading(true);
      const url = await loadPhotoSafely(imgbbUrl);
      setPhotoUrl(url);

      if (!navigator.onLine) {
        const isCached = await isPhotoCached(imgbbUrl);
        setCached(isCached);
      }

      setLoading(false);
    };

    loadPhoto();
  }, [imgbbUrl]);

  return { photoUrl, loading, cached, isOnline: navigator.onLine };
};
