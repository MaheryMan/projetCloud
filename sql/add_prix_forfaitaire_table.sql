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
