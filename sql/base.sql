-- Active: 1749037938113@@127.0.0.1@5432@cloud
-- =========================
-- TABLE ROLES
-- =========================
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(50),
    niveau INTEGER
);

-- =========================
-- TABLE STATUS
-- =========================
CREATE TABLE status (
    id SERIAL PRIMARY KEY,
    status_id VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(50) NOT NULL
);

-- =========================
-- TABLE SOURCES
-- =========================
CREATE TABLE sources (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(50)
);

-- =========================
-- TABLE NIVEAU_TRAVAUX
-- =========================
CREATE TABLE niveau_travaux (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(50) NOT NULL,
    niveau INTEGER NOT NULL
);

 -- =========================
 -- TABLE: TYPES_SIGNALEMENT
 -- =========================
 CREATE TABLE types_signalement (
     id SERIAL PRIMARY KEY,
     libelle VARCHAR(100) NOT NULL UNIQUE,
     description TEXT,
     icone VARCHAR(100),
     couleur VARCHAR(20),
     niveau_urgence INTEGER DEFAULT 2,
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
-- TABLE UTILISATEURS
-- =========================
CREATE TABLE utilisateurs (
    id SERIAL PRIMARY KEY,
    email VARCHAR(50) UNIQUE,
    num_tel VARCHAR(50) UNIQUE,
    password TEXT,
    nom VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    tentatives INTEGER DEFAULT 0,
    cree_le TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_le TIMESTAMP,
    delete_le TIMESTAMP,
    id_source INTEGER NOT NULL,
    id_status INTEGER NOT NULL,
    CONSTRAINT fk_source
        FOREIGN KEY (id_source) REFERENCES sources(id),
    CONSTRAINT fk_status
        FOREIGN KEY (id_status) REFERENCES status(id)
);

-- =========================
-- TABLE DEBLOCAGES
-- =========================
CREATE TABLE deblocages (
    id SERIAL PRIMARY KEY,
    date_deblocage TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    motif TEXT,
    id_utilisateur_bloque INTEGER NOT NULL,
    id_manager INTEGER NOT NULL,
    CONSTRAINT fk_user_bloque
        FOREIGN KEY (id_utilisateur_bloque) REFERENCES utilisateurs(id),
    CONSTRAINT fk_manager
        FOREIGN KEY (id_manager) REFERENCES utilisateurs(id)
);

-- =========================
-- TABLE SESSIONS
-- =========================
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token TEXT UNIQUE NOT NULL,
    cree_le TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expire_le TIMESTAMP NOT NULL,
    est_valide BOOLEAN NOT NULL DEFAULT TRUE,
    id_utilisateur INTEGER NOT NULL,
    CONSTRAINT fk_session_user
        FOREIGN KEY (id_utilisateur) REFERENCES utilisateurs(id)
);

-- =========================
-- TABLE SIGNALEMENTS
-- =========================
CREATE TABLE signalements (
    id SERIAL PRIMARY KEY,
    latitude NUMERIC(10,6) NOT NULL,
    longitude NUMERIC(10,6) NOT NULL,
    surface_m2 NUMERIC(15,2),
    date_signalement TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description TEXT,
    id_niveau_travaux INTEGER NOT NULL,
     id_type_signalement INTEGER,
    id_utilisateur INTEGER NOT NULL,
    CONSTRAINT fk_niveau_travaux
        FOREIGN KEY (id_niveau_travaux) REFERENCES niveau_travaux(id),
     CONSTRAINT fk_type_signalement
         FOREIGN KEY (id_type_signalement) REFERENCES types_signalement(id),
    CONSTRAINT fk_signalement_user
        FOREIGN KEY (id_utilisateur) REFERENCES utilisateurs(id)
);

 -- Migration: affecter le type 'Autre' aux signalements existants puis rendre obligatoire
 UPDATE signalements
 SET id_type_signalement = (SELECT id FROM types_signalement WHERE libelle = 'Autre')
 WHERE id_type_signalement IS NULL;
 
 ALTER TABLE signalements
 ALTER COLUMN id_type_signalement SET NOT NULL;

-- =========================
-- TABLE HISTORIQUES_STATUS_SIGNALEMENT
-- =========================
CREATE TABLE historiques_status_signalement (
    id SERIAL PRIMARY KEY,
    date_historique TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_signalement INTEGER NOT NULL,
    id_status INTEGER NOT NULL,
    CONSTRAINT fk_hist_signalement
        FOREIGN KEY (id_signalement) REFERENCES signalements(id),
    CONSTRAINT fk_hist_status
        FOREIGN KEY (id_status) REFERENCES status(id)
);

-- =========================
-- TABLE USERS_ROLES
-- =========================
CREATE TABLE users_roles (
    id_utilisateur INTEGER NOT NULL,
    id_role INTEGER NOT NULL,
    PRIMARY KEY (id_utilisateur, id_role),
    CONSTRAINT fk_ur_user
        FOREIGN KEY (id_utilisateur) REFERENCES utilisateurs(id),
    CONSTRAINT fk_ur_role
        FOREIGN KEY (id_role) REFERENCES roles(id)
);
