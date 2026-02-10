import React, { useState, useEffect } from 'react';
import './PrixForfaitaire.css';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

const PrixForfaitaire = () => {
    const [prixActif, setPrixActif] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);
    const [formData, setFormData] = useState({
        prixParMetreCarre: '',
        multiplicateurNiveau: ''
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
                    prixParMetreCarre: data.prixParMetreCarre,
                    multiplicateurNiveau: data.multiplicateurNiveau
                });
            } else {
                // Aucun prix actif trouvé, on initialise avec des valeurs par défaut
                setFormData({
                    prixParMetreCarre: '100.00',
                    multiplicateurNiveau: '1.50'
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
            });

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
            return null;
        }

        const prix = parseFloat(formData.prixParMetreCarre);
        const mult = parseFloat(formData.multiplicateurNiveau);
        const surface = 50; // Exemple avec 50m²

        return {
            niveau1: (surface * prix * Math.pow(mult, 1)).toFixed(2),
            niveau2: (surface * prix * Math.pow(mult, 2)).toFixed(2),
            niveau3: (surface * prix * Math.pow(mult, 3)).toFixed(2)
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

                    <div className="form-group">
                        <label htmlFor="multiplicateurNiveau">
                            Multiplicateur de Niveau
                        </label>
                        <input
                            type="number"
                            id="multiplicateurNiveau"
                            name="multiplicateurNiveau"
                            value={formData.multiplicateurNiveau}
                            onChange={handleChange}
                            step="0.01"
                            min="0.01"
                            required
                            className="form-input"
                        />
                        <small className="form-hint">
                            Multiplicateur appliqué selon le niveau d'urgence (ex: 1.5)
                        </small>
                    </div>

                    <button type="submit" className="btn-submit">
                        {prixActif ? 'Mettre à Jour le Prix' : 'Créer le Prix'}
                    </button>
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

                <div className="info-section">
                    <h3>📋 Information</h3>
                    <p>
                        Le budget des signalements est calculé automatiquement selon la formule :
                    </p>
                    <code className="formule">
                        Budget = Surface × Prix par m² × (Multiplicateur<sup>Niveau d'urgence</sup>)
                    </code>
                    <ul className="info-list">
                        <li>Le prix est appliqué automatiquement lors de la création d'un signalement</li>
                        <li>Le multiplicateur augmente le prix selon le niveau d'urgence</li>
                        <li>Les anciens prix sont archivés et peuvent être consultés</li>
                    </ul>
                </div>
            </div>
        </div>
    );
};

export default PrixForfaitaire;
