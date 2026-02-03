-- Migration pour ajouter l'entité Photo et modifier les signalements
-- Date: 2026-02-03
-- Version: 2.0 - Support pour plusieurs photos par signalement

-- Création de la table photos
CREATE TABLE IF NOT EXISTS photos (
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

-- Ajout d'index pour optimiser les recherches
CREATE INDEX IF NOT EXISTS idx_photos_url ON photos(url);
CREATE INDEX IF NOT EXISTS idx_photos_file_name ON photos(file_name);
CREATE INDEX IF NOT EXISTS idx_photos_mime_type ON photos(mime_type);
CREATE INDEX IF NOT EXISTS idx_photos_created_at ON photos(created_at);
CREATE INDEX IF NOT EXISTS idx_photos_signalement_id ON photos(id_signalement);

-- NOTE: La colonne id_photo dans signalements n'est plus nécessaire
-- car maintenant c'est la table photos qui référence signalements
-- Si vous avez déjà créé id_photo, vous pouvez la supprimer :
-- ALTER TABLE signalements DROP COLUMN IF EXISTS id_photo;

-- Migration des données existantes: si vous avez des photoUrl dans vos données
-- Cette partie est optionnelle et dépend de votre structure actuelle

-- Fonction pour migrer les photoUrl existantes (si applicable)
-- Vous devrez adapter cette partie selon vos besoins existants

-- Commentaires pour les administrateurs :
-- 1. Cette migration ajoute une nouvelle entité Photo séparée
-- 2. Les signalements peuvent maintenant être liés à une photo via id_photo
-- 3. La relation est optionnelle (peut être NULL)
-- 4. Les anciennes références directes à photoUrl doivent être remplacées par cette relation

-- Trigger pour mettre à jour automatiquement updated_at dans photos
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_photos_updated_at 
    BEFORE UPDATE ON photos 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();