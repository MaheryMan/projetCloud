-- Migration pour ajouter la table prix_forfaitaire
-- Cette table gère les prix forfaitaires pour le calcul automatique du budget des signalements

-- Création de la table prix_forfaitaire
CREATE TABLE IF NOT EXISTS prix_forfaitaire (
    id BIGSERIAL PRIMARY KEY,
    prix_par_metre_carre NUMERIC(15, 2) NOT NULL,
    multiplicateur_niveau NUMERIC(5, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

-- Commentaires sur la table et les colonnes
COMMENT ON TABLE prix_forfaitaire IS 'Table de gestion des prix forfaitaires pour le calcul automatique du budget';
COMMENT ON COLUMN prix_forfaitaire.prix_par_metre_carre IS 'Prix de base par mètre carré';
COMMENT ON COLUMN prix_forfaitaire.multiplicateur_niveau IS 'Multiplicateur appliqué selon le niveau d''urgence (multiplicateur ^ niveau)';
COMMENT ON COLUMN prix_forfaitaire.created_at IS 'Date de création du prix';
COMMENT ON COLUMN prix_forfaitaire.deleted_at IS 'Date de désactivation du prix (soft delete)';

-- Index pour améliorer les performances
CREATE INDEX idx_prix_forfaitaire_deleted_at ON prix_forfaitaire(deleted_at);
CREATE INDEX idx_prix_forfaitaire_created_at ON prix_forfaitaire(created_at DESC);

-- Insertion d'un prix par défaut (à adapter selon vos besoins)
-- Prix de base: 100 par mètre carré
-- Multiplicateur: 1.5 (niveau 1 = 1.5x, niveau 2 = 2.25x, niveau 3 = 3.375x, etc.)
INSERT INTO prix_forfaitaire (prix_par_metre_carre, multiplicateur_niveau, created_at, deleted_at)
VALUES (100.00, 1.50, CURRENT_TIMESTAMP, NULL);

-- Remarque: Le budget est maintenant calculé automatiquement selon la formule:
-- budget = surface_m2 * prix_par_metre_carre * (multiplicateur_niveau ^ niveau_urgence)
