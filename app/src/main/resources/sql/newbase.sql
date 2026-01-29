-- =========================
-- TABLE: SOURCES (authentification)
-- =========================
CREATE TABLE sources (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(50) NOT NULL,
    provider_type VARCHAR(20) NOT NULL, -- 'firebase_email', 'google', 'local'
    is_online BOOLEAN DEFAULT TRUE,     -- TRUE pour Firebase/Google, FALSE pour local
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Valeurs par défaut
INSERT INTO sources (libelle, provider_type, is_online) VALUES
('Firebase Email/Mot de passe', 'firebase_email', TRUE),
('Google OAuth', 'google', TRUE),
('Base locale', 'local', FALSE);

-- =========================
-- TABLE: STATUS (statuts utilisateurs + signalements)
-- =========================
CREATE TABLE status (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(50) NOT NULL
);

-- Statuts utilisateurs
INSERT INTO status (code, libelle) VALUES
('USER_ACTIF', 'Actif'),
('USER_BLOQUE', 'Bloqué'),
('USER_INACTIF', 'Inactif');

-- Statuts signalements
INSERT INTO status (code, libelle) VALUES
('REPORT_NOUVEAU', 'Nouveau'),
('REPORT_EN_COURS', 'En cours'),
('REPORT_TERMINE', 'Terminé'),
('REPORT_ANNULE', 'Annulé');

-- =========================
-- TABLE: ENTREPRISES
-- =========================
CREATE TABLE entreprises (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    contact_telephone VARCHAR(50),
    adresse TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Entreprise par défaut
INSERT INTO entreprises (nom) VALUES ('Entreprise par défaut');

-- =========================
-- TABLE: ROLES
-- =========================
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(50) NOT NULL UNIQUE,
    niveau INTEGER NOT NULL  -- Pour l'ordre hiérarchique
);

-- Rôles disponibles
INSERT INTO roles (libelle, niveau) VALUES
('Manager', 10),     -- Haut niveau
('Mobile_User', 5),  -- Utilisateur mobile
('Visiteur', 0);     -- Pour logique métier (facultatif)

-- =========================
-- TABLE: UTILISATEURS
-- =========================
CREATE TABLE utilisateurs (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password TEXT,                    -- NULL pour Firebase/Google
    firebase_uid VARCHAR(128) UNIQUE, -- ID Firebase si en ligne
    nom VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    num_tel VARCHAR(50),
    
    -- Gestion sécurité
    tentatives_connexion INTEGER DEFAULT 0,
    is_blocked BOOLEAN DEFAULT FALSE,
    last_failed_attempt TIMESTAMP,
    
    -- Références
    id_source INTEGER NOT NULL REFERENCES sources(id),
    id_status INTEGER NOT NULL REFERENCES status(id),
    
    -- Synchronisation
    is_synced_to_firebase BOOLEAN DEFAULT FALSE,
    last_synced_at TIMESTAMP,
    firebase_created_at TIMESTAMP,    -- Date création sur Firebase
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    
    -- Contraintes
    CONSTRAINT check_password_source CHECK (
        (id_source IN (SELECT id FROM sources WHERE provider_type = 'local') AND password IS NOT NULL) OR
        (id_source NOT IN (SELECT id FROM sources WHERE provider_type = 'local') AND password IS NULL)
    )
);

-- Créer le compte manager par défaut (local)
INSERT INTO utilisateurs (email, password, nom, prenom, id_source, id_status) 
VALUES (
    'manager@admin.com',
    -- Mot de passe hashé: 'admin123' (à changer en production)
    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'Admin',
    'Manager',
    (SELECT id FROM sources WHERE provider_type = 'local'),
    (SELECT id FROM status WHERE code = 'USER_ACTIF')
);

-- =========================
-- TABLE: USER_ROLES (liaison utilisateurs-rôles)
-- =========================
CREATE TABLE user_roles (
    id_utilisateur INTEGER NOT NULL REFERENCES utilisateurs(id),
    id_role INTEGER NOT NULL REFERENCES roles(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_utilisateur, id_role)
);

-- Assigner rôle Manager au compte par défaut
INSERT INTO user_roles (id_utilisateur, id_role)
VALUES (
    (SELECT id FROM utilisateurs WHERE email = 'manager@admin.com'),
    (SELECT id FROM roles WHERE libelle = 'Manager')
);

-- =========================
-- TABLE: SESSIONS
-- =========================
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token TEXT UNIQUE NOT NULL,
    id_utilisateur INTEGER NOT NULL REFERENCES utilisateurs(id),
    device_info TEXT,
    ip_address VARCHAR(45),
    
    -- Durée de vie
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- État
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    logout_at TIMESTAMP,
    
    -- Index pour performance
    INDEX idx_sessions_user (id_utilisateur),
    INDEX idx_sessions_expires (expires_at)
);

-- =========================
-- TABLE: DEBLOCAGES
-- =========================
CREATE TABLE deblocages (
    id SERIAL PRIMARY KEY,
    id_utilisateur INTEGER NOT NULL REFERENCES utilisateurs(id),
    id_manager INTEGER NOT NULL REFERENCES utilisateurs(id),
    motif TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Contrainte: le manager doit avoir le rôle Manager
    CONSTRAINT fk_manager_role CHECK (
        EXISTS (
            SELECT 1 FROM user_roles ur 
            JOIN roles r ON ur.id_role = r.id 
            WHERE ur.id_utilisateur = id_manager AND r.libelle = 'Manager'
        )
    ),
    
    INDEX idx_deblocages_user (id_utilisateur),
    INDEX idx_deblocages_manager (id_manager)
);

-- =========================
-- TABLE: SIGNALEMENTS
-- =========================
CREATE TABLE signalements (
    id SERIAL PRIMARY KEY,
    
    -- Position
    latitude NUMERIC(10,6) NOT NULL,
    longitude NUMERIC(10,6) NOT NULL,
    
    -- Description
    description TEXT NOT NULL,
    photo_url TEXT,                   -- URL de la photo si uploadée
    
    -- Détails techniques
    surface_m2 NUMERIC(15,2),
    budget NUMERIC(15,2),
    
    -- Références
    id_entreprise INTEGER REFERENCES entreprises(id),
    id_utilisateur INTEGER NOT NULL REFERENCES utilisateurs(id), -- Celui qui a signalé
    id_status INTEGER NOT NULL REFERENCES status(id),            -- Statut courant
    
    -- Synchronisation Firebase
    is_synced_to_firebase BOOLEAN DEFAULT FALSE,
    firebase_id VARCHAR(128),         -- ID correspondant dans Firebase
    synced_at TIMESTAMP,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    -- Index pour recherche géospatiale
    INDEX idx_signalements_location (latitude, longitude),
    INDEX idx_signalements_user (id_utilisateur),
    INDEX idx_signalements_status (id_status),
    
    -- Contraintes
    CONSTRAINT valid_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT valid_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT positive_surface CHECK (surface_m2 IS NULL OR surface_m2 >= 0),
    CONSTRAINT positive_budget CHECK (budget IS NULL OR budget >= 0)
);

-- =========================
-- TABLE: HISTORIQUES_STATUS_SIGNALEMENT
-- =========================
CREATE TABLE historiques_status_signalement (
    id SERIAL PRIMARY KEY,
    id_signalement INTEGER NOT NULL REFERENCES signalements(id),
    id_status INTEGER NOT NULL REFERENCES status(id),
    id_utilisateur INTEGER NOT NULL REFERENCES utilisateurs(id), -- Qui a changé le statut
    commentaire TEXT,                                            -- Optionnel: pourquoi le changement
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_historiques_signalement (id_signalement),
    INDEX idx_historiques_date (created_at)
);

-- =========================
-- TABLE: CONFIGURATIONS (paramètres système)
-- =========================
CREATE TABLE configurations (
    id SERIAL PRIMARY KEY,
    cle VARCHAR(100) NOT NULL UNIQUE,
    valeur TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Paramètres par défaut
INSERT INTO configurations (cle, valeur, description) VALUES
('tentatives_max', '3', 'Nombre maximum de tentatives de connexion avant blocage'),
('duree_session_minutes', '1440', 'Durée de validité d''une session en minutes (24h)'),
('duree_blocage_minutes', '30', 'Durée du blocage après trop de tentatives'),
('sync_auto', 'false', 'Synchronisation automatique avec Firebase'),
('version_api', '1.0.0', 'Version actuelle de l''API');

-- =========================
-- VUES UTILES
-- =========================

-- Vue: Utilisateurs avec leurs rôles
CREATE VIEW vue_utilisateurs_complets AS
SELECT 
    u.id,
    u.email,
    u.nom,
    u.prenom,
    u.num_tel,
    u.tentatives_connexion,
    u.is_blocked,
    s.libelle as source_auth,
    st.libelle as statut_utilisateur,
    STRING_AGG(r.libelle, ', ') as roles,
    u.created_at,
    u.updated_at
FROM utilisateurs u
JOIN sources s ON u.id_source = s.id
JOIN status st ON u.id_status = st.id
LEFT JOIN user_roles ur ON u.id = ur.id_utilisateur
LEFT JOIN roles r ON ur.id_role = r.id
GROUP BY u.id, s.libelle, st.libelle;

-- Vue: Signalements avec toutes les informations
CREATE VIEW vue_signalements_complets AS
SELECT 
    s.id,
    s.latitude,
    s.longitude,
    s.description,
    s.surface_m2,
    s.budget,
    st.libelle as statut,
    e.nom as entreprise,
    CONCAT(u.prenom, ' ', u.nom) as signalant,
    u.email as email_signalant,
    s.created_at,
    s.updated_at
FROM signalements s
JOIN status st ON s.id_status = st.id
LEFT JOIN entreprises e ON s.id_entreprise = e.id
JOIN utilisateurs u ON s.id_utilisateur = u.id;

-- Vue: Statistiques globales
CREATE VIEW vue_statistiques AS
SELECT 
    COUNT(*) as total_signalements,
    COUNT(CASE WHEN id_status = (SELECT id FROM status WHERE code = 'REPORT_NOUVEAU') THEN 1 END) as nouveaux,
    COUNT(CASE WHEN id_status = (SELECT id FROM status WHERE code = 'REPORT_EN_COURS') THEN 1 END) as en_cours,
    COUNT(CASE WHEN id_status = (SELECT id FROM status WHERE code = 'REPORT_TERMINE') THEN 1 END) as termines,
    COALESCE(SUM(surface_m2), 0) as surface_totale_m2,
    COALESCE(SUM(budget), 0) as budget_total,
    COALESCE(AVG(budget), 0) as budget_moyen
FROM signalements;

-- =========================
-- FONCTIONS ET TRIGGERS
-- =========================

-- Fonction: Mettre à jour updated_at automatiquement
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers pour les tables principales
CREATE TRIGGER update_utilisateurs_updated_at 
    BEFORE UPDATE ON utilisateurs 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_signalements_updated_at 
    BEFORE UPDATE ON signalements 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_entreprises_updated_at 
    BEFORE UPDATE ON entreprises 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Fonction: Loguer les changements de statut automatiquement
CREATE OR REPLACE FUNCTION log_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.id_status IS DISTINCT FROM NEW.id_status THEN
        INSERT INTO historiques_status_signalement 
        (id_signalement, id_status, id_utilisateur, commentaire)
        VALUES (
            NEW.id, 
            NEW.id_status, 
            NEW.id_utilisateur,  -- Supposons que le manager qui modifie est dans le contexte
            'Changement automatique via trigger'
        );
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER log_signalement_status_change
    AFTER UPDATE OF id_status ON signalements
    FOR EACH ROW EXECUTE FUNCTION log_status_change();

-- =========================
-- INDEX SUPPLÉMENTAIRES
-- =========================

-- Pour les recherches textuelles
CREATE INDEX idx_utilisateurs_email ON utilisateurs USING HASH (email);
CREATE INDEX idx_utilisateurs_nom_prenom ON utilisateurs (nom, prenom);

-- Pour les rapports et statistiques
CREATE INDEX idx_signalements_date ON signalements (created_at);
CREATE INDEX idx_signalements_surface ON signalements (surface_m2) WHERE surface_m2 IS NOT NULL;
CREATE INDEX idx_signalements_budget ON signalements (budget) WHERE budget IS NOT NULL;

-- Pour la gestion des sessions expirées
CREATE INDEX idx_sessions_validite ON sessions (is_valid, expires_at);

-- =========================
-- COMMENTAIRES
-- =========================

COMMENT ON TABLE utilisateurs IS 'Utilisateurs de l''application (managers et utilisateurs mobiles)';
COMMENT ON TABLE signalements IS 'Signalements de problèmes routiers';
COMMENT ON TABLE historiques_status_signalement IS 'Historique des changements de statut des signalements';
COMMENT ON TABLE deblocages IS 'Journal des déblocages d''utilisateurs par les managers';
COMMENT ON TABLE configurations IS 'Paramètres de configuration de l''application';

COMMENT ON COLUMN utilisateurs.firebase_uid IS 'ID unique Firebase (NULL si utilisateur local)';
COMMENT ON COLUMN utilisateurs.is_synced_to_firebase IS 'TRUE si synchronisé avec Firebase';
COMMENT ON COLUMN signalements.firebase_id IS 'ID correspondant dans Firebase (pour sync)';
COMMENT ON COLUMN signalements.is_synced_to_firebase IS 'TRUE si le signalement est synchronisé avec Firebase';
COMMENT ON COLUMN configurations.cle IS 'Clé de configuration (ex: tentatives_max)';

-- =========================
-- TABLE: TYPES_SIGNALEMENT
-- =========================
CREATE TABLE types_signalement (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icone VARCHAR(100),              -- Nom de l'icône (ex: "pothole", "diversion")
    couleur VARCHAR(20),             -- Couleur associée sur la carte
    niveau_urgence INTEGER DEFAULT 2, -- 1=Urgent, 2=Normal, 3=Faible
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Types par défaut
INSERT INTO types_signalement (libelle, description, icone, couleur, niveau_urgence) VALUES
('Trou / Nid-de-poule', 'Dégradation de la chaussée avec creux', 'pothole', '#FF0000', 1),
('Déviation / Chantier', 'Travaux en cours avec déviation', 'diversion', '#FFA500', 2),
('Signalisation manquante', 'Panneau de signalisation absent ou endommagé', 'sign', '#FFFF00', 2),
('Éclairage défaillant', 'Lampadaire public non fonctionnel', 'light', '#0000FF', 3),
('Déchets sur la voie', 'Objets ou déchets obstruant la circulation', 'trash', '#808080', 2),
('Inondation', 'Eau stagnante sur la chaussée', 'flood', '#0000FF', 1),
('Revêtement dégradé', 'Chaussée abîmée mais sans trou', 'road', '#800000', 2),
('Végétation envahissante', 'Arbres/plantes obstruant la voie', 'tree', '#008000', 3),
('Autre', 'Autre type de problème non catégorisé', 'other', '#666666', 2);

-- =========================
-- MODIFIER TABLE SIGNALEMENTS
-- =========================
ALTER TABLE signalements 
ADD COLUMN id_type_signalement INTEGER REFERENCES types_signalement(id);

-- Mettre à jour les signalements existants (optionnel)
UPDATE signalements 
SET id_type_signalement = (SELECT id FROM types_signalement WHERE libelle = 'Autre')
WHERE id_type_signalement IS NULL;

-- Rendre la colonne obligatoire après migration
ALTER TABLE signalements 
ALTER COLUMN id_type_signalement SET NOT NULL;

-- =========================
-- METTRE À JOUR LA VUE
-- =========================
DROP VIEW IF EXISTS vue_signalements_complets;
CREATE VIEW vue_signalements_complets AS
SELECT 
    s.id,
    s.latitude,
    s.longitude,
    s.description,
    ts.libelle as type_signalement,
    ts.icone,
    ts.couleur,
    s.surface_m2,
    s.budget,
    st.libelle as statut,
    e.nom as entreprise,
    CONCAT(u.prenom, ' ', u.nom) as signalant,
    u.email as email_signalant,
    s.created_at,
    s.updated_at
FROM signalements s
JOIN status st ON s.id_status = st.id
JOIN types_signalement ts ON s.id_type_signalement = ts.id
LEFT JOIN entreprises e ON s.id_entreprise = e.id
JOIN utilisateurs u ON s.id_utilisateur = u.id;

-- =========================
-- VUE: STATISTIQUES PAR TYPE
-- =========================
CREATE VIEW vue_statistiques_par_type AS
SELECT 
    ts.libelle as type_signalement,
    COUNT(s.id) as nombre,
    COUNT(CASE WHEN st.code = 'REPORT_NOUVEAU' THEN 1 END) as nouveaux,
    COUNT(CASE WHEN st.code = 'REPORT_EN_COURS' THEN 1 END) as en_cours,
    COUNT(CASE WHEN st.code = 'REPORT_TERMINE' THEN 1 END) as termines,
    COALESCE(SUM(s.surface_m2), 0) as surface_totale_m2,
    COALESCE(SUM(s.budget), 0) as budget_total,
    ROUND(COUNT(CASE WHEN st.code = 'REPORT_TERMINE' THEN 1 END) * 100.0 / COUNT(s.id), 1) as taux_achevement
FROM signalements s
JOIN types_signalement ts ON s.id_type_signalement = ts.id
JOIN status st ON s.id_status = st.id
GROUP BY ts.id, ts.libelle
ORDER BY COUNT(s.id) DESC;