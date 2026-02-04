import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);

/**
 * Enregistrer le Service Worker pour le caching offline des images ImgBB
 */
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker
      .register('/service-worker.js')
      .then((registration) => {
        console.log('[App] Service Worker registered:', registration);
      })
      .catch((error) => {
        console.warn('[App] Service Worker registration failed:', error);
      });
  });
} else {
  console.warn('[App] Service Workers not supported in this browser');
}
