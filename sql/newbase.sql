-- =========================
-- TABLE: SOURCES (authentification)
-- =========================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE sources (
    id BIGSERIAL PRIMARY KEY,
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
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(50) NOT NULL
);

-- Statuts utilisateurs
INSERT INTO status (code, libelle) VALUES
('USR001', 'Actif'),
('USR002', 'Bloqué'),
('USR003', 'Inactif');

-- Statuts signalements
INSERT INTO status (code, libelle) VALUES
('REPORT001', 'Nouveau'),
('REPORT002', 'En cours'),
('REPORT003', 'Terminé'),
('REPORT004', 'Annulé'),
('REPORT005', 'Créé');

-- =========================
-- TABLE: ENTREPRISES
-- =========================
CREATE TABLE entreprises (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    contact_telephone VARCHAR(50),
    adresse TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Entreprise par défaut
INSERT INTO entreprises (nom, contact_email, contact_telephone, adresse) VALUES 
('Entreprise par défaut', 'default@entreprise.com', '+261 34 12 345 67', 'Adresse par défaut, Ville, Pays'),
('Magic', 'contact@magic.com', '+261 34 98 765 43', '123 Rue Magique, Antananarivo, Madagascar'),
('Colas', 'info@colas.com', '+261 34 56 789 01', '456 Avenue Colas, Toamasina, Madagascar');


-- =========================
-- TABLE: ROLES
-- =========================
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
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
    id BIGSERIAL PRIMARY KEY,
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
    id_source BIGINT NOT NULL REFERENCES sources(id),
    id_status BIGINT NOT NULL REFERENCES status(id),
    
    -- Synchronisation
    is_synced_to_firebase BOOLEAN DEFAULT FALSE,
    last_synced_at TIMESTAMP,
    firebase_created_at TIMESTAMP,    -- Date création sur Firebase
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    
    -- Synchronisation avancée
    modified_offline BOOLEAN DEFAULT FALSE,
    last_modified_at TIMESTAMP,
    temp_password TEXT                -- Mot de passe temporaire pour sync offline
);

-- Créer le compte manager par défaut (local)
-- IMPORTANT: Si cet utilisateur existe déjà dans Firebase:
-- 1. Le script crée d'abord l'utilisateur dans PostgreSQL sans firebase_uid
-- 2. Au démarrage de l'application, syncUsersToFirebase() détectera que manager@admin.com existe dans Firebase
-- 3. Il récupérera le firebase_uid de Firebase et l'ajoutera à PostgreSQL
-- 4. L'utilisateur sera marqué comme isSyncedToFirebase = TRUE
INSERT INTO utilisateurs (email, password, nom, prenom, id_source, id_status, temp_password) 
VALUES (
    'manager@admin.com',
    -- Mot de passe hashé: 'admin123' (à changer en production)
    crypt('admin123', gen_salt('bf', 10)),
    'Admin',
    'Manager',
    (SELECT id FROM sources WHERE provider_type = 'local'),
    (SELECT id FROM status WHERE code = 'USR001'),
    'admin123'  -- Temporairement stocker le mot de passe en clair pour la sync
);

-- =========================
-- TABLE: USER_ROLES (liaison utilisateurs-rôles)
-- =========================
CREATE TABLE user_roles (
    id_utilisateur BIGINT NOT NULL REFERENCES utilisateurs(id),
    id_role BIGINT NOT NULL REFERENCES roles(id),
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
    id_utilisateur BIGINT NOT NULL REFERENCES utilisateurs(id),
    device_info TEXT,
    ip_address VARCHAR(45),
    
    -- Durée de vie
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- État
    is_valid BOOLEAN NOT NULL DEFAULT TRUE,
    logout_at TIMESTAMP
);

-- Index pour sessions
CREATE INDEX idx_sessions_user ON sessions (id_utilisateur);
CREATE INDEX idx_sessions_expires ON sessions (expires_at);

-- =========================
-- TABLE: DEBLOCAGES
-- =========================
CREATE TABLE deblocages (
    id BIGSERIAL PRIMARY KEY,
    id_utilisateur BIGINT NOT NULL REFERENCES utilisateurs(id),
    id_manager BIGINT NOT NULL REFERENCES utilisateurs(id),
    motif TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index pour deblocages
CREATE INDEX idx_deblocages_user ON deblocages (id_utilisateur);
CREATE INDEX idx_deblocages_manager ON deblocages (id_manager);

-- =========================
-- TABLE: TYPES_SIGNALEMENT
-- =========================
CREATE TABLE types_signalement (
    id BIGSERIAL PRIMARY KEY,
    libelle VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icone VARCHAR(100),              -- Nom de l'icône (ex: "pothole", "diversion")
    couleur VARCHAR(20),             -- Couleur associée sur la carte
    niveau_urgence INTEGER DEFAULT 2, -- 1=Urgent, 2=Normal, 3=Faible
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Types par défaut
INSERT INTO types_signalement (libelle, description, icone, couleur, niveau_urgence) VALUES
('Trou', 'Dégradation de la chaussée avec creux', 'pothole', '#FF0000', 1),
('Chantier', 'Travaux en cours avec déviation', 'construction', '#FFA500', 2),
('Autre', 'Autre type de problème non catégorisé', 'other', '#666666', 2);

-- =========================
-- TABLE: SIGNALEMENTS
-- =========================
CREATE TABLE signalements (
    id BIGSERIAL PRIMARY KEY,
    
    -- Position
    latitude NUMERIC(10,6) NOT NULL,
    longitude NUMERIC(10,6) NOT NULL,
    
    -- Description
    description TEXT NOT NULL,
    
    -- Détails techniques
    surface_m2 NUMERIC(15,2),
    budget NUMERIC(15,2),
    
    -- Références
    id_type_signalement BIGINT NOT NULL REFERENCES types_signalement(id),
    id_entreprise BIGINT REFERENCES entreprises(id),
    id_utilisateur BIGINT NOT NULL REFERENCES utilisateurs(id), -- Celui qui a signalé
    id_status BIGINT NOT NULL REFERENCES status(id),            -- Statut courant
    
    -- Synchronisation Firebase
    is_synced_to_firebase BOOLEAN DEFAULT FALSE,
    firebase_id VARCHAR(128),         -- ID correspondant dans Firebase
    synced_at TIMESTAMP,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    -- Contraintes
    CONSTRAINT valid_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT valid_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT positive_surface CHECK (surface_m2 IS NULL OR surface_m2 >= 0),
    CONSTRAINT positive_budget CHECK (budget IS NULL OR budget >= 0)
);

-- =========================
-- TABLE: PHOTOS (entité séparée pour plusieurs photos par signalement)
-- =========================

-- Fonction pour mettre à jour automatiquement updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TABLE photos (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(1000) NOT NULL,
    description VARCHAR(500),
    file_name VARCHAR(255),
    file_size BIGINT,
    mime_type VARCHAR(100),
    uploaded_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- Relation vers signalement (plusieurs photos par signalement)
    id_signalement BIGINT REFERENCES signalements(id) ON DELETE CASCADE
);

-- Index pour photos
CREATE INDEX idx_photos_url ON photos(url);
CREATE INDEX idx_photos_file_name ON photos(file_name);
CREATE INDEX idx_photos_mime_type ON photos(mime_type);
CREATE INDEX idx_photos_created_at ON photos(created_at);
CREATE INDEX idx_photos_signalement_id ON photos(id_signalement);

-- Trigger pour mettre à jour updated_at dans photos
CREATE TRIGGER update_photos_updated_at 
    BEFORE UPDATE ON photos 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- Index pour signalements
CREATE INDEX idx_signalements_location ON signalements (latitude, longitude);
CREATE INDEX idx_signalements_user ON signalements (id_utilisateur);
CREATE INDEX idx_signalements_status ON signalements (id_status);
CREATE INDEX idx_signalements_date ON signalements (created_at);
CREATE INDEX idx_signalements_surface ON signalements (surface_m2) WHERE surface_m2 IS NOT NULL;
CREATE INDEX idx_signalements_budget ON signalements (budget) WHERE budget IS NOT NULL;

-- =========================
-- TABLE: HISTORIQUES_STATUS_SIGNALEMENT
-- =========================
CREATE TABLE historiques_status_signalement (
    id BIGSERIAL PRIMARY KEY,
    id_signalement BIGINT NOT NULL REFERENCES signalements(id),
    id_status BIGINT NOT NULL REFERENCES status(id),
    id_utilisateur BIGINT NOT NULL REFERENCES utilisateurs(id), -- Qui a changé le statut
    commentaire TEXT,                                            -- Optionnel: pourquoi le changement
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index pour historiques
CREATE INDEX idx_historiques_signalement ON historiques_status_signalement (id_signalement);
CREATE INDEX idx_historiques_date ON historiques_status_signalement (created_at);

-- =========================
-- TABLE: CONFIGURATIONS (paramètres système)
-- =========================
CREATE TABLE configurations (
    id BIGSERIAL PRIMARY KEY,
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
-- INDEX SUPPLÉMENTAIRES
-- =========================

-- Pour les recherches textuelles
CREATE INDEX idx_utilisateurs_email ON utilisateurs USING HASH (email);
CREATE INDEX idx_utilisateurs_nom_prenom ON utilisateurs (nom, prenom);

-- Pour la gestion des sessions expirées
CREATE INDEX idx_sessions_validite ON sessions (is_valid, expires_at);

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

-- Vue: Statistiques globales
CREATE VIEW vue_statistiques AS
SELECT 
    COUNT(*) as total_signalements,
    COUNT(CASE WHEN id_status = (SELECT id FROM status WHERE code = 'REPORT001') THEN 1 END) as nouveaux,
    COUNT(CASE WHEN id_status = (SELECT id FROM status WHERE code = 'REPORT002') THEN 1 END) as en_cours,
    COUNT(CASE WHEN id_status = (SELECT id FROM status WHERE code = 'REPORT003') THEN 1 END) as termines,
    COALESCE(SUM(surface_m2), 0) as surface_totale_m2,
    COALESCE(SUM(budget), 0) as budget_total,
    COALESCE(AVG(budget), 0) as budget_moyen
FROM signalements;

-- Vue: Statistiques par type
CREATE VIEW vue_statistiques_par_type AS
SELECT 
    ts.libelle as type_signalement,
    COUNT(s.id) as nombre,
    COUNT(CASE WHEN st.code = 'REPORT001' THEN 1 END) as nouveaux,
    COUNT(CASE WHEN st.code = 'REPORT002' THEN 1 END) as en_cours,
    COUNT(CASE WHEN st.code = 'REPORT003' THEN 1 END) as termines,
    COALESCE(SUM(s.surface_m2), 0) as surface_totale_m2,
    COALESCE(SUM(s.budget), 0) as budget_total,
    ROUND(COUNT(CASE WHEN st.code = 'REPORT003' THEN 1 END) * 100.0 / NULLIF(COUNT(s.id), 0), 1) as taux_achevement
FROM signalements s
JOIN types_signalement ts ON s.id_type_signalement = ts.id
JOIN status st ON s.id_status = st.id
GROUP BY ts.id, ts.libelle
ORDER BY COUNT(s.id) DESC;

-- =========================
-- FONCTIONS ET TRIGGERS
-- =========================

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
            NEW.id_utilisateur,
            'Changement automatique via trigger'
        );
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER log_signalement_status_change
    AFTER UPDATE OF id_status ON signalements
    FOR EACH ROW EXECUTE FUNCTION log_status_change();

-- Fonction: Créer un historique lors de la création d'un signalement
CREATE OR REPLACE FUNCTION create_initial_historique()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO historiques_status_signalement 
    (id_signalement, id_status, id_utilisateur, commentaire)
    VALUES (
        NEW.id, 
        NEW.id_status, 
        NEW.id_utilisateur,
        'Création du signalement'
    );
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER log_signalement_creation
    AFTER INSERT ON signalements
    FOR EACH ROW EXECUTE FUNCTION create_initial_historique();

-- =========================
-- FONCTION: Validation password pour utilisateurs locaux
-- =========================
CREATE OR REPLACE FUNCTION validate_user_password()
RETURNS TRIGGER AS $$
DECLARE
    src_type VARCHAR(20);
BEGIN
    -- Récupérer le type de provider
    SELECT provider_type INTO src_type 
    FROM sources 
    WHERE id = NEW.id_source;
    
    -- Vérifier la cohérence password/source
    IF src_type = 'local' AND NEW.password IS NULL THEN
        RAISE EXCEPTION 'Un utilisateur local doit avoir un mot de passe';
    END IF;
    
    -- Pour Firebase/Google, permettre password hashé pour fallback local
    
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER check_user_password
    BEFORE INSERT OR UPDATE ON utilisateurs
    FOR EACH ROW EXECUTE FUNCTION validate_user_password();

-- =========================
-- FONCTION: Validation manager pour déblocages
-- =========================
CREATE OR REPLACE FUNCTION validate_manager_role()
RETURNS TRIGGER AS $$
BEGIN
    -- Vérifier que l'utilisateur qui débloque a bien le rôle Manager
    IF NOT EXISTS (
        SELECT 1 FROM user_roles ur 
        JOIN roles r ON ur.id_role = r.id 
        WHERE ur.id_utilisateur = NEW.id_manager 
        AND r.libelle = 'Manager'
    ) THEN
        RAISE EXCEPTION 'Seul un utilisateur avec le rôle Manager peut débloquer un compte';
    END IF;
    
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER check_manager_role
    BEFORE INSERT ON deblocages
    FOR EACH ROW EXECUTE FUNCTION validate_manager_role();

-- =========================
-- COLONNES DE SYNCHRONISATION FIREBASE
-- =========================

-- Ajouter colonnes de sync pour status
ALTER TABLE status ADD COLUMN IF NOT EXISTS is_synced_to_firebase BOOLEAN DEFAULT FALSE;
ALTER TABLE status ADD COLUMN IF NOT EXISTS last_synced_at TIMESTAMP;

-- Ajouter colonnes de sync pour entreprises
ALTER TABLE entreprises ADD COLUMN IF NOT EXISTS is_synced_to_firebase BOOLEAN DEFAULT FALSE;
ALTER TABLE entreprises ADD COLUMN IF NOT EXISTS last_synced_at TIMESTAMP;

-- Ajouter colonnes de sync pour types_signalement
ALTER TABLE types_signalement ADD COLUMN IF NOT EXISTS is_synced_to_firebase BOOLEAN DEFAULT FALSE;
ALTER TABLE types_signalement ADD COLUMN IF NOT EXISTS last_synced_at TIMESTAMP;

-- Ajouter colonne pour tracer la synchro depuis Firebase
ALTER TABLE signalements ADD COLUMN IF NOT EXISTS synced_from_firebase_at TIMESTAMP;
ALTER TABLE signalements ADD COLUMN IF NOT EXISTS needs_firebase_sync BOOLEAN DEFAULT FALSE;

-- Index pour optimiser les requêtes de synchro
CREATE INDEX IF NOT EXISTS idx_signalements_firebase_id ON signalements (firebase_id);
CREATE INDEX IF NOT EXISTS idx_signalements_needs_sync ON signalements (needs_firebase_sync) WHERE needs_firebase_sync = TRUE;

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

-- PATCH : Correction migration pour Hibernate
-- 1. Supprimer la vue avant modification
DROP VIEW IF EXISTS vue_utilisateurs_complets;

-- 2. Modifier la colonne num_tel (exemple : changer le type)
ALTER TABLE utilisateurs ALTER COLUMN num_tel TYPE varchar(255);

-- 3. Ajouter une colonne NOT NULL à status (exemple)
ALTER TABLE status ADD COLUMN IF NOT EXISTS status_id varchar(50);
UPDATE status SET status_id = 'default' WHERE status_id IS NULL;
ALTER TABLE status ALTER COLUMN status_id SET NOT NULL;

-- 4. Recréer la vue après modification
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


-- Migration pour ajouter la table prix_forfaitaire
-- Cette table gère les prix forfaitaires pour le calcul automatique du budget des signalements

-- Création de la table prix_forfaitaire
CREATE TABLE IF NOT EXISTS prix_forfaitaire (
    id BIGSERIAL PRIMARY KEY,
    prix_par_metre_carre NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

-- Commentaires sur la table et les colonnes
COMMENT ON TABLE prix_forfaitaire IS 'Table de gestion des prix forfaitaires pour le calcul automatique du budget';
COMMENT ON COLUMN prix_forfaitaire.prix_par_metre_carre IS 'Prix de base par mètre carré';
COMMENT ON COLUMN prix_forfaitaire.created_at IS 'Date de création du prix';
COMMENT ON COLUMN prix_forfaitaire.deleted_at IS 'Date de désactivation du prix (soft delete)';

-- Index pour améliorer les performances
CREATE INDEX idx_prix_forfaitaire_deleted_at ON prix_forfaitaire(deleted_at);
CREATE INDEX idx_prix_forfaitaire_created_at ON prix_forfaitaire(created_at DESC);

-- Insertion d'un prix par défaut (à adapter selon vos besoins)
-- Prix de base: 100 par mètre carré
INSERT INTO prix_forfaitaire (prix_par_metre_carre, created_at, deleted_at)
VALUES (100.00, CURRENT_TIMESTAMP, NULL);

-- Ajouter la colonne niveau dans la table signalements si elle n'existe pas déjà
ALTER TABLE signalements ADD COLUMN IF NOT EXISTS niveau INTEGER;

-- Commentaire sur la nouvelle colonne
COMMENT ON COLUMN signalements.niveau IS 'Niveau du signalement (1, 2, 3, etc.)';

-- Remarque: Le budget est maintenant calculé automatiquement selon la formule:
-- budget = prix_par_m2 * niveau * surface_m2

