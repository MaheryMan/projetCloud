import React, { useState, useEffect } from 'react';
import './PrixForfaitaire.css';

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
            const response = await fetch(`${API_URL}/api/prix-forfaitaire/actif`);
            
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

            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    prixParMetreCarre: parseFloat(formData.prixParMetreCarre),
                    multiplicateurNiveau: parseFloat(formData.multiplicateurNiveau)
                })

            if (response.ok) {
                const data = await response.json();
                setPrixActif(data);
                setSuccess('Prix forfaitaire mis à jour avec succès!');
                setTimeout(() => setSuccess(null), 3000);
            } else {
                const errorData = await response.json();
                setError(errorData.message || 'Erreur lors de la mise à jour du prix');
            }
        } catch (err) {
            setError('Erreur lors de la communication avec le serveur');
            console.error(err);
        }
    };

    const calculerExemple = () => {
        if (!formData.prixParMetreCarre || !formData.multiplicateurNiveau) {
            return null;) {
            return null;
        }

        const prix = parseFloat(formData.prixParMetreCarre);
        const surface = 50; // Exemple avec 50m²

        return {
            niveau1: (prix * 1 * surface).toFixed(2),
            niveau2: (prix * 2 * surface).toFixed(2),
            niveau3: (prix * 3 * surface
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

                    <div className="form-group">
                        <label htmlFor="multiplicateurNiveau">
                     /button>
                </form>

                {exemples && (
                    <div className="exemples-section">
                        <h3>Exemples de Calcul (pour 50m²)</h3>
                        <div className="exemples-grid">
                            <div className="exemple-card">
                                <div className="exemple-niveau">Niveau 1</div>
                                <div className="exemple-prix">{exemples.niveau1} Ar</div>
                                <div className="exemple-formule">
                                    50 × {formData.prixParMetreCarre} × {formData.multiplicateurNiveau}<sup>1</sup>
                                </div>
                            </div>
                            <div className="exemple-card">
                                <div className="exemple-niveau">Niveau 2</div>
                                <div className="exemple-prix">{exemples.niveau2} Ar</div>
                                <div className="exemple-formule">
                                    50 × {formData.prixParMetreCarre} × {formData.multiplicateurNiveau}<sup>2</sup>
                                </div>
                            </div>
                            <div className="exemple-card">
                                <div className="exemple-niveau">Niveau 3</div>
                                <div className="exemple-prix">{exemples.niveau3} Ar</div>
                                <div className="exemple-formule">
                                    50 × {formData.prixParMetreCarre} × {formData.multiplicateurNiveau}<sup>3</sup>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                <div className="info{formData.prixParMetreCarre} × 1 × 50
                                </div>
                            </div>
                            <div className="exemple-card">
                                <div className="exemple-niveau">Niveau 2</div>
                                <div className="exemple-prix">{exemples.niveau2} Ar</div>
                                <div className="exemple-formule">
                                    {formData.prixParMetreCarre} × 2 × 50
                                </div>
                            </div>
                            <div className="exemple-card">
                                <div className="exemple-niveau">Niveau 3</div>
                                <div className="exemple-prix">{exemples.niveau3} Ar</div>
                                <div className="exemple-formule">
                                    {formData.prixParMetreCarre} × 3 × 50
        </div>
    );
};

export default PrixForfaitaire;
Prix par m² × Niveau × Surface
                    </code>
                    <ul className="info-list">
                        <li>Le prix est appliqué automatiquement lors de la création d'un signalement</li>
                        <li>Le niveau est saisi pour chaque signalement