import React, { useEffect, useState } from 'react';
import './FirebaseStatusBubble.css';

function FirebaseStatusBubble() {
  const [isOnline, setIsOnline] = useState(null);

  useEffect(() => {
    let intervalId;
    const checkConnectivity = async () => {
      try {
        const res = await fetch('http://localhost:8080/api/connectivity/firebase');
        const online = await res.json();
        setIsOnline(online === true);
      } catch {
        setIsOnline(false);
      }
    };
    checkConnectivity();
    intervalId = setInterval(checkConnectivity, 10000);
    return () => clearInterval(intervalId);
  }, []);

  return (
    <div className="firebase-status-bubble-fixed">
      <span
        className={`status-bubble ${isOnline === true ? 'online' : isOnline === false ? 'offline' : ''}`}
        title={isOnline === true ? 'Connecté à Firebase' : isOnline === false ? 'Hors ligne Firebase' : 'Vérification...'}
      ></span>
      <span className="status-label">
        {isOnline === true ? 'En ligne' : isOnline === false ? 'Hors ligne' : '...'}
      </span>
    </div>
  );
}

export default FirebaseStatusBubble;
