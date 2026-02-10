import React, { useState, useEffect } from 'react';
import './PrixForfaitaire.css';
import { fetchWithAuth } from '../services/authService';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

const PrixForfaitaire = () => {
    const [prixActif, setPrixActif] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);
    const [formData, setFormData] = useState({
        prixParMetreCarre: ''
    });

    // Charger le prix actif au montage du composant
    useEffect(() => {
        fetchActivePrix();
    }, []);

    const fetchActivePrix = async () => {
        try {
            setLoading(true);
            const response = await fetchWithAuth(`${API_URL}/api/prix-forfaitaire/actif`);
            
            if (response.ok) {
                const data = await response.json();
                setPrixActif(data);
                setFormData({
                    prixParMetreCarre: data.prixParMetreCarre
                });
            } else {
                // Aucun prix actif trouvé, on initialise avec des valeurs par défaut
                setFormData({
                    prixParMetreCarre: '100.00'
                });
            }
        } catch (err) {
            setError('Erreur lors du chargement du prix actif');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setSuccess(null);

        try {
            const url = prixActif 
                ? `${API_URL}/api/prix-forfaitaire` 
                : `${API_URL}/api/prix-forfaitaire`;
            
            const method = prixActif ? 'PUT' : 'POST';

            console.log('Envoi de la requête:', { url, method, data: { prixParMetreCarre: parseFloat(formData.prixParMetreCarre) } });

            const response = await fetchWithAuth(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    prixParMetreCarre: parseFloat(formData.prixParMetreCarre)
                })
            });

            console.log('Réponse reçue:', { status: response.status, ok: response.ok });

            if (response.ok) {
                const data = await response.json();
                console.log('Données reçues:', data);
                setPrixActif(data);
                setSuccess('Prix forfaitaire mis à jour avec succès!');
                setTimeout(() => setSuccess(null), 3000);
            } else {
                let errorMessage = 'Erreur lors de la mise à jour du prix';
                try {
                    const errorData = await response.json();
                    errorMessage = errorData.message || errorMessage;
                } catch (parseError) {
                    console.error('Erreur lors du parsing de la réponse d\'erreur:', parseError);
                    errorMessage = `Erreur ${response.status}: ${response.statusText}`;
                }
                console.error('Erreur API:', errorMessage);
                setError(errorMessage);
            }
        } catch (err) {
            console.error('Erreur de communication:', err);
            setError(`Erreur lors de la communication avec le serveur: ${err.message}`);
        }
    };

    const calculerExemple = () => {
        if (!formData.prixParMetreCarre) {
            return null;
        }

        const prix = parseFloat(formData.prixParMetreCarre);
        const surface = 50; // Exemple avec 50m²

        return {
            niveau1: (prix * 1 * surface).toFixed(2),
            niveau2: (prix * 2 * surface).toFixed(2),
            niveau3: (prix * 3 * surface).toFixed(2)
        };
    };

    const exemples = calculerExemple();

    if (loading) {
        return <div className="loading">Chargement...</div>;
    }

    return (
        <div className="prix-forfaitaire-container">
            <div className="prix-forfaitaire-card">
                <h1>Gestion des Prix Forfaitaires</h1>
                
                {error && <div className="alert alert-error">{error}</div>}
                {success && <div className="alert alert-success">{success}</div>}

                <form onSubmit={handleSubmit} className="prix-form">
                    <div className="form-group">
                        <label htmlFor="prixParMetreCarre">
                            Prix par Mètre Carré (Ar)
                        </label>
                        <input
                            type="number"
                            id="prixParMetreCarre"
                            name="prixParMetreCarre"
                            value={formData.prixParMetreCarre}
                            onChange={handleChange}
                            step="0.01"
                            min="0.01"
                            required
                            className="form-input"
                        />
                        <small className="form-hint">
                            Prix de base appliqué par mètre carré de surface
                        </small>
                    </div>

                    <button type="submit" className="btn btn-primary">
                        {prixActif ? 'Mettre à jour le prix' : 'Créer le prix'}
                    </button>
                </form>



            </div>
        </div>
    );
};

export default PrixForfaitaire;
