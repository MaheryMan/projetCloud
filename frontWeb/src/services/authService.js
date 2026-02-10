/**
 * Service d'authentification et gestion de session
 */

const API_URL = 'http://localhost:8080/api';

/**
 * Déconnecte l'utilisateur et nettoie le localStorage
 */
export const logout = () => {
  const token = localStorage.getItem('token');
  
  // Appeler l'API de déconnexion côté serveur
  if (token) {
    fetch(`${API_URL}/auth/logout`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }).catch(err => console.error('Erreur lors de la déconnexion:', err));
  }
  
  // Nettoyer le localStorage
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  
  // Émettre un événement personnalisé pour notifier les autres composants
  window.dispatchEvent(new CustomEvent('localStorageChange', { 
    detail: { key: 'logout' } 
  }));
  
  // Rediriger vers la page de login
  window.location.href = '/login';
};

/**
 * Vérifie si le token est toujours valide
 * @returns {Promise<boolean>}
 */
export const checkTokenValidity = async () => {
  const token = localStorage.getItem('token');
  if (!token) return false;

  try {
    // Utiliser l'endpoint de validation de session
    const response = await fetch(`${API_URL}/sessions/validate`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    if (response.status === 401 || response.status === 403) {
      // Token invalide ou expiré
      const errorData = await response.text();
      console.warn('Session invalide:', errorData);
      logout();
      return false;
    }

    return response.ok;
  } catch (error) {
    console.error('Erreur lors de la vérification du token:', error);
    return false;
  }
};

/**
 * Intercepteur pour les requêtes fetch
 * Déconnecte automatiquement si 401 ou 403
 */
export const fetchWithAuth = async (url, options = {}) => {
  const token = localStorage.getItem('token');
  
  const headers = {
    ...options.headers,
    'Authorization': token ? `Bearer ${token}` : ''
  };

  try {
    const response = await fetch(url, {
      ...options,
      headers
    });

    // Déconnexion automatique si session expirée
    if (response.status === 401 || response.status === 403) {
      const errorText = await response.text();
      if (errorText.includes('expiré') || errorText.includes('invalide')) {
        console.warn('Session expirée, déconnexion automatique');
        logout();
      }
    }

    return response;
  } catch (error) {
    console.error('Erreur réseau:', error);
    throw error;
  }
};

/**
 * Démarre un timer pour vérifier périodiquement la validité du token
 * @param {number} intervalMinutes - Intervalle de vérification en minutes (défaut: 5 minutes)
 */
export const startSessionMonitoring = (intervalMinutes = 5) => {
  // Vérifier immédiatement
  checkTokenValidity();
  
  // Vérifier périodiquement
  const intervalId = setInterval(() => {
    checkTokenValidity();
  }, intervalMinutes * 60 * 1000);

  // Retourner l'ID pour pouvoir arrêter le monitoring si nécessaire
  return intervalId;
};

/**
 * Arrête le monitoring de session
 */
export const stopSessionMonitoring = (intervalId) => {
  if (intervalId) {
    clearInterval(intervalId);
  }
};
