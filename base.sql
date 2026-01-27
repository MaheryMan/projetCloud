CREATE TABLE roles(
   id INTEGER,
   libelle VARCHAR(50) ,
   niveau INTEGER,
   PRIMARY KEY(id)
);

CREATE TABLE status(
   id INTEGER,
   status_id VARCHAR(50)  NOT NULL,
   libelle VARCHAR(50)  NOT NULL,
   PRIMARY KEY(id),
   UNIQUE(status_id)
);

CREATE TABLE sources(
   id INTEGER,
   libelle VARCHAR(50) ,
   PRIMARY KEY(id)
);

CREATE TABLE niveau_travaux(
   id INTEGER,
   libelle VARCHAR(50)  NOT NULL,
   niveau INTEGER NOT NULL,
   PRIMARY KEY(id)
);

CREATE TABLE utilisateurs (
   id              SERIAL PRIMARY KEY,
   email           VARCHAR(255) UNIQUE,
   num_tel         VARCHAR(50) UNIQUE,
   password_hash   TEXT,                 -- hash BCrypt (nullable si Google-only)
   firebase_uid    VARCHAR(128),        -- id Firebase, nullable au début
   nom             VARCHAR(255) NOT NULL,
   prenom          VARCHAR(255) NOT NULL,
   tentatives      INTEGER DEFAULT 0,
   cree_le         TIMESTAMP DEFAULT now(),
   update_le       TIMESTAMP,
   delete_le       TIMESTAMP,
   id_source       INTEGER NOT NULL,
   id_status       INTEGER NOT NULL,
   CONSTRAINT uq_firebase_uid UNIQUE (firebase_uid),
   FOREIGN KEY(id_source) REFERENCES sources(id),
   FOREIGN KEY(id_status) REFERENCES status(id)
);

CREATE TABLE deblocages(
   id INTEGER,
   date_deblocage TIMESTAMP,
   motif TEXT,
   id_utilisateur_bloque INTEGER NOT NULL,
   id_manager INTEGER NOT NULL,
   PRIMARY KEY(id),
   FOREIGN KEY(id_utilisateur_bloque) REFERENCES utilisateurs(id),
   FOREIGN KEY(id_manager) REFERENCES utilisateurs(id)
);

CREATE TABLE sessions(
   id VARCHAR(50) ,
   token TEXT,
   cree_le TIMESTAMP,
   expire_le TIMESTAMP,
   est_valide BOOLEAN NOT NULL,
   id_utilisateur INTEGER NOT NULL,
   PRIMARY KEY(id),
   UNIQUE(token),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateurs(id)
);

CREATE TABLE signalements(
   id INTEGER,
   latitude NUMERIC(10,10)   NOT NULL,
   surface_m2 NUMERIC(15,2)  ,
   longitude NUMERIC(10,10)   NOT NULL,
   date_signalement TIMESTAMP,
   description TEXT,
   id_niveau_travaux INTEGER NOT NULL,
   id_utilisateur INTEGER NOT NULL,
   PRIMARY KEY(id),
   FOREIGN KEY(id_niveau_travaux) REFERENCES niveau_travaux(id),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateurs(id)
);

CREATE TABLE historiques_status_signalement(
   id INTEGER,
   date_historique TIMESTAMP NOT NULL,
   id_signalement INTEGER NOT NULL,
   id_status INTEGER NOT NULL,
   PRIMARY KEY(id),
   FOREIGN KEY(id_signalement) REFERENCES signalements(id),
   FOREIGN KEY(id_status) REFERENCES status(id)
);

CREATE TABLE users_roles (
   id_utilisateur  INTEGER,
   id_role         INTEGER,
   PRIMARY KEY(id_utilisateur, id_role),
   FOREIGN KEY(id_utilisateur) REFERENCES utilisateurs(id),
   FOREIGN KEY(id_role) REFERENCES roles(id)
);
