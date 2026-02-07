import React, { useState, useEffect } from 'react';
import { getPhotoCacheStats, clearPhotoCache } from '../services/photoCache';
import { FaBox, FaTrash } from 'react-icons/fa';

/**
 * Composant pour afficher les stats du cache des photos
 * Utile pour le debugging et la gestion du stockage
 */
export function PhotoCacheStats() {
  const [stats, setStats] = useState({ count: 0, size: 0, sizeInMB: '0' });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadStats();
    // Rafraîchir les stats toutes les 30 secondes
    const interval = setInterval(loadStats, 30000);
    return () => clearInterval(interval);
  }, []);

  const loadStats = async () => {
    const newStats = await getPhotoCacheStats();
    setStats(newStats);
    setLoading(false);
  };

  const handleClearCache = async () => {
    if (window.confirm('Êtes-vous sûr de vouloir supprimer le cache des photos?')) {
      await clearPhotoCache();
      setStats({ count: 0, size: 0, sizeInMB: '0' });
    }
  };

  if (loading) {
    return <div className="cache-stats">Chargement...</div>;
  }

  return (
    <div className="cache-stats">
      <div className="cache-stats-content">
        <h3><FaBox /> Cache Photos Offline</h3>
        <ul>
          <li>
            <strong>Photos en cache:</strong> {stats.count}
          </li>
          <li>
            <strong>Espace utilisé:</strong> {stats.sizeInMB} MB
          </li>
          <li>
            <strong>Limite:</strong> 50 MB
          </li>
        </ul>
        {stats.count > 0 && (
          <button onClick={handleClearCache} className="cache-clear-btn">
            <FaTrash /> Vider le cache
          </button>
        )}
      </div>
    </div>
  );
}

export default PhotoCacheStats;
