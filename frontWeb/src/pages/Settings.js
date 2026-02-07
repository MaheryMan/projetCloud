import React, { useEffect, useState } from 'react';
import './Settings.css';

function Settings() {
  const [activeTab, setActiveTab] = useState('types-signalement');
  const [typesSignalement, setTypesSignalement] = useState([]);
  const [entreprises, setEntreprises] = useState([]);
  const [statuses, setStatuses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // États pour les formulaires
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({});

  useEffect(() => {
    fetchAllData();
  }, []);

  const fetchAllData = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      const headers = token ? { 'Authorization': `Bearer ${token}` } : {};

      const [typesRes, entreprisesRes, statusesRes] = await Promise.all([
        fetch('http://localhost:8080/api/types-signalement', { headers }),
        fetch('http://localhost:8080/api/entreprises', { headers }),
        fetch('http://localhost:8080/api/status', { headers })
      ]);

      if (!typesRes.ok || !entreprisesRes.ok || !statusesRes.ok) {
        throw new Error('Erreur lors du chargement des données');
      }

      const typesData = await typesRes.json();
      const entreprisesData = await entreprisesRes.json();
      const statusesData = await statusesRes.json();

      setTypesSignalement(Array.isArray(typesData) ? typesData : []);
      setEntreprises(Array.isArray(entreprisesData) ? entreprisesData : []);
      setStatuses(Array.isArray(statusesData) ? statusesData : []);
      setError('');
    } catch (err) {
      console.error('Erreur:', err);
      setError('Impossible de charger les données. Vérifiez votre connexion.');
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => {
    setFormData({});
    setEditingId(null);
    setShowForm(false);
  };

  // ===== SYNCHRONISATION MÉTADONNÉES =====
  const handleSyncMetadata = async () => {
    if (!window.confirm('Synchroniser les métadonnées (Status, Entreprises, Types de signalement, Configurations)?\n\nCette opération synchronise les paramètres entre PostgreSQL et Firebase (bidirectionnel).')) {
      return;
    }

    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      const res = await fetch('http://localhost:8080/api/sync/metadata', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.message || `Erreur HTTP ${res.status}`);
      }

      alert(`✅ Synchronisation réussie!\n${data.count} élément(s) de configuration synchronisé(s)`);
      await fetchAllData(); // Rafraîchir les données
    } catch (error) {
      console.error('Erreur:', error);
      alert(`❌ Erreur lors de la synchronisation des métadonnées: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      const token = localStorage.getItem('token');
      const headers = {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
      };

      let url = '';
      let method = 'POST';

      if (activeTab === 'types-signalement') {
        url = editingId
          ? `http://localhost:8080/api/types-signalement/${editingId}`
          : 'http://localhost:8080/api/types-signalement';
        method = editingId ? 'PUT' : 'POST';
      } else if (activeTab === 'entreprises') {
        url = editingId
          ? `http://localhost:8080/api/entreprises/${editingId}`
          : 'http://localhost:8080/api/entreprises';
        method = editingId ? 'PUT' : 'POST';
      } else if (activeTab === 'status') {
        url = editingId
          ? `http://localhost:8080/api/status/${editingId}`
          : 'http://localhost:8080/api/status';
        method = editingId ? 'PUT' : 'POST';
      }

      const response = await fetch(url, {
        method,
        headers,
        body: JSON.stringify(formData)
      });

      if (!response.ok) {
        throw new Error('Erreur lors de la sauvegarde');
      }

      alert(editingId ? 'Mise à jour réussie' : 'Ajout réussi');
      await fetchAllData();
      resetForm();
    } catch (err) {
      console.error('Erreur:', err);
      alert('Erreur lors de la sauvegarde');
    }
  };

  const handleEdit = (item) => {
    setFormData(item);
    setEditingId(item.id);
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Êtes-vous sûr de vouloir supprimer cet élément ?')) {
      return;
    }

    try {
      const token = localStorage.getItem('token');
      const headers = token ? { 'Authorization': `Bearer ${token}` } : {};

      let url = '';
      if (activeTab === 'types-signalement') {
        url = `http://localhost:8080/api/types-signalement/${id}`;
      } else if (activeTab === 'entreprises') {
        url = `http://localhost:8080/api/entreprises/${id}`;
      } else if (activeTab === 'status') {
        url = `http://localhost:8080/api/status/${id}`;
      }

      const response = await fetch(url, {
        method: 'DELETE',
        headers
      });

      if (!response.ok) {
        throw new Error('Erreur lors de la suppression');
      }

      alert('Suppression réussie');
      await fetchAllData();
    } catch (err) {
      console.error('Erreur:', err);
      alert('Erreur lors de la suppression');
    }
  };

  if (loading) {
    return <div className="loading">Chargement...</div>;
  }

  return (
    <div className="settings">
      <header className="page-header">
        <h1>⚙️ Paramètres</h1>
        <p>Gérer les types de signalements, les entreprises et les statuts</p>
      </header>

      {/* Synchronisation Section */}
      <div style={{ 
        backgroundColor: 'white', 
        border: '2px solid #e2e8f0', 
        borderRadius: '12px', 
        padding: '20px', 
        marginBottom: '24px',
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.05)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
          <div style={{ flex: 1 }}>
            <h4 style={{ color: '#0f172a', marginTop: 0, marginBottom: '8px', fontWeight: 700, fontSize: '16px' }}>🔄 Synchroniser les Paramètres</h4>
            <p style={{ color: '#475569', fontSize: '14px', margin: 0, lineHeight: 1.6 }}>
              Synchronisez les métadonnées (statuts, entreprises, types de signalement) entre PostgreSQL et Firebase
            </p>
          </div>
          <button
            onClick={handleSyncMetadata}
            disabled={loading}
            title="Synchroniser tous les paramètres système"
            style={{
              background: 'linear-gradient(135deg, #2563eb, #1e40af)',
              color: 'white',
              padding: '12px 24px',
              border: 'none',
              borderRadius: '8px',
              cursor: loading ? 'not-allowed' : 'pointer',
              fontSize: '14px',
              fontWeight: '600',
              opacity: loading ? 0.6 : 1,
              whiteSpace: 'nowrap',
              boxShadow: loading ? 'none' : '0 2px 8px rgba(37, 99, 235, 0.3)',
              transition: 'all 0.2s ease'
            }}
          >
            {loading ? '⏳ Synchronisation...' : '⚙️ Synchroniser'}
          </button>
        </div>
      </div>

      <div className="tabs-container">
        <div className="tabs-nav">
          <button
            className={`tab-btn ${activeTab === 'types-signalement' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('types-signalement');
              resetForm();
            }}
          >
            📋 Types de signalement ({typesSignalement.length})
          </button>
          <button
            className={`tab-btn ${activeTab === 'entreprises' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('entreprises');
              resetForm();
            }}
          >
            🏢 Entreprises ({entreprises.length})
          </button>
          <button
            className={`tab-btn ${activeTab === 'status' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('status');
              resetForm();
            }}
          >
            ✅ Statuts ({statuses.length})
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="content-area">
        {/* Types de signalement */}
        {activeTab === 'types-signalement' && (
          <div className="tab-content">
            <div className="section-header">
              <h2>📋 Types de signalement</h2>
              <button
                className="btn-primary"
                onClick={() => {
                  setFormData({});
                  setEditingId(null);
                  setShowForm(!showForm);
                }}
              >
                + Ajouter un type
              </button>
            </div>

            {showForm && (
              <form onSubmit={handleSubmit} className="form-container">
                <h3>{editingId ? 'Modifier le type' : 'Nouveau type de signalement'}</h3>
                
                <div className="form-group">
                  <label>Libellé *</label>
                  <input
                    type="text"
                    placeholder="Ex: Trou / Nid-de-poule"
                    value={formData.libelle || ''}
                    onChange={(e) => setFormData({ ...formData, libelle: e.target.value })}
                    required
                  />
                </div>

                <div className="form-group">
                  <label>Description</label>
                  <textarea
                    placeholder="Décrivez ce type de signalement"
                    value={formData.description || ''}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    rows="3"
                  />
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>Icône</label>
                    <input
                      type="text"
                      placeholder="Ex: pothole"
                      value={formData.icone || ''}
                      onChange={(e) => setFormData({ ...formData, icone: e.target.value })}
                    />
                  </div>

                  <div className="form-group">
                    <label>Couleur</label>
                    <input
                      type="color"
                      value={formData.couleur || '#FF0000'}
                      onChange={(e) => setFormData({ ...formData, couleur: e.target.value })}
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label>Niveau d'urgence</label>
                  <select
                    value={formData.niveauUrgence || 2}
                    onChange={(e) => setFormData({ ...formData, niveauUrgence: parseInt(e.target.value) })}
                  >
                    <option value={1}>1 - Urgent</option>
                    <option value={2}>2 - Normal</option>
                    <option value={3}>3 - Faible</option>
                  </select>
                </div>

                <div className="form-actions">
                  <button type="submit" className="btn-save">Enregistrer</button>
                  <button type="button" className="btn-cancel" onClick={resetForm}>
                    Annuler
                  </button>
                </div>
              </form>
            )}

            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Libellé</th>
                    <th>Description</th>
                    <th>Urgence</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {typesSignalement.map((type) => (
                    <tr key={type.id}>
                      <td>#{type.id}</td>
                      <td><strong>{type.libelle}</strong></td>
                      <td>{type.description || 'N/A'}</td>
                    
                      <td>
                        <span className={`urgency-badge urgency-${type.niveauUrgence || 2}`}>
                          {type.niveauUrgence === 1 ? ' Urgent' : type.niveauUrgence === 3 ? ' Faible' : ' Normal'}
                        </span>
                      </td>
                      <td>
                        <div className="action-buttons">
                          <button
                            className="btn-edit"
                            onClick={() => handleEdit(type)}
                            title="Modifier"
                          >
                             Modifier
                          </button>
                          <button
                            className="btn-delete"
                            onClick={() => handleDelete(type.id)}
                            title="Supprimer"
                          >
                             Supprimer
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {typesSignalement.length === 0 && (
                <div className="no-results">Aucun type de signalement trouvé</div>
              )}
            </div>
          </div>
        )}

        {/* Entreprises */}
        {activeTab === 'entreprises' && (
          <div className="tab-content">
            <div className="section-header">
              <h2>🏢 Entreprises</h2>
              <button
                className="btn-primary"
                onClick={() => {
                  setFormData({});
                  setEditingId(null);
                  setShowForm(!showForm);
                }}
              >
                + Ajouter une entreprise
              </button>
            </div>

            {showForm && (
              <form onSubmit={handleSubmit} className="form-container">
                <h3>{editingId ? 'Modifier l\'entreprise' : 'Nouvelle entreprise'}</h3>
                
                <div className="form-group">
                  <label>Nom *</label>
                  <input
                    type="text"
                    placeholder="Ex: Entreprise ABC"
                    value={formData.nom || ''}
                    onChange={(e) => setFormData({ ...formData, nom: e.target.value })}
                    required
                  />
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>Email</label>
                    <input
                      type="email"
                      placeholder="contact@entreprise.com"
                      value={formData.contactEmail || ''}
                      onChange={(e) => setFormData({ ...formData, contactEmail: e.target.value })}
                    />
                  </div>

                  <div className="form-group">
                    <label>Téléphone</label>
                    <input
                      type="tel"
                      placeholder="+261 XX XXX XX"
                      value={formData.contactTelephone || ''}
                      onChange={(e) => setFormData({ ...formData, contactTelephone: e.target.value })}
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label>Adresse</label>
                  <textarea
                    placeholder="Adresse complète"
                    value={formData.adresse || ''}
                    onChange={(e) => setFormData({ ...formData, adresse: e.target.value })}
                    rows="3"
                  />
                </div>

                <div className="form-actions">
                  <button type="submit" className="btn-save">Enregistrer</button>
                  <button type="button" className="btn-cancel" onClick={resetForm}>
                    Annuler
                  </button>
                </div>
              </form>
            )}

            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Nom</th>
                    <th>Email</th>
                    <th>Téléphone</th>
                    <th>Adresse</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {entreprises.map((entreprise) => (
                    <tr key={entreprise.id}>
                      <td>#{entreprise.id}</td>
                      <td><strong>{entreprise.nom}</strong></td>
                      <td>{entreprise.contactEmail || 'N/A'}</td>
                      <td>{entreprise.contactTelephone || 'N/A'}</td>
                      <td>{entreprise.adresse ? entreprise.adresse.substring(0, 30) + '...' : 'N/A'}</td>
                      <td>
                        <div className="action-buttons">
                          <button
                            className="btn-edit"
                            onClick={() => handleEdit(entreprise)}
                            title="Modifier"
                          >
                             Modifier
                          </button>
                          <button
                            className="btn-delete"
                            onClick={() => handleDelete(entreprise.id)}
                            title="Supprimer"
                          >
                             Supprimer
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {entreprises.length === 0 && (
                <div className="no-results">Aucune entreprise trouvée</div>
              )}
            </div>
          </div>
        )}

        {/* Status */}
        {activeTab === 'status' && (
          <div className="tab-content">
            <div className="section-header">
              <h2>✅ Statuts</h2>
              <button
                className="btn-primary"
                onClick={() => {
                  setFormData({});
                  setEditingId(null);
                  setShowForm(!showForm);
                }}
              >
                + Ajouter un statut
              </button>
            </div>

            {showForm && (
              <form onSubmit={handleSubmit} className="form-container">
                <h3>{editingId ? 'Modifier le statut' : 'Nouveau statut'}</h3>
                
                <div className="form-group">
                  <label>Code *</label>
                  <input
                    type="text"
                    placeholder="Ex: USR001"
                    value={formData.code || ''}
                    onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                    required
                  />
                </div>

                <div className="form-group">
                  <label>Libellé *</label>
                  <input
                    type="text"
                    placeholder="Ex: Actif"
                    value={formData.libelle || ''}
                    onChange={(e) => setFormData({ ...formData, libelle: e.target.value })}
                    required
                  />
                </div>

                <div className="form-actions">
                  <button type="submit" className="btn-save">Enregistrer</button>
                  <button type="button" className="btn-cancel" onClick={resetForm}>
                    Annuler
                  </button>
                </div>
              </form>
            )}

            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Code</th>
                    <th>Libellé</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {statuses.map((status) => (
                    <tr key={status.id}>
                      <td>#{status.id}</td>
                      <td><code>{status.code}</code></td>
                      <td><strong>{status.libelle}</strong></td>
                      <td>
                        <div className="action-buttons">
                          <button
                            className="btn-edit"
                            onClick={() => handleEdit(status)}
                            title="Modifier"
                          >
                             Modifier
                          </button>
                          <button
                            className="btn-delete"
                            onClick={() => handleDelete(status.id)}
                            title="Supprimer"
                          >
                             Supprimer
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {statuses.length === 0 && (
                <div className="no-results">Aucun statut trouvé</div>
              )}
            </div>
          </div>
        )}
      </div>

      <div className="info-box">
        <h3>ℹ️ Informations</h3>
        <ul>
          <li><strong>Types de signalement:</strong> Ajoutez ou modifiez les catégories de problèmes routiers</li>
          <li><strong>Entreprises:</strong> Gérez les entreprises responsables des réparations</li>
          <li><strong>Statuts:</strong> Définissez les états possibles des signalements et utilisateurs</li>
          <li>Tous les champs marqués d'un <strong>*</strong> sont obligatoires</li>
          <li><strong>Synchronisation:</strong> Utilisez le bouton en haut pour synchroniser les paramètres avec Firebase</li>
        </ul>
      </div>
    </div>
  );
}

export default Settings;
