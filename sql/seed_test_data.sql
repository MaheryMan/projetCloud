-- Données de test (PostgreSQL)
-- Objectif:
-- - créer des valeurs minimales dans sources/status/roles
-- - créer des utilisateurs de test avec delete_le = NULL
-- - générer un hash BCrypt en base via pgcrypto (crypt + gen_salt('bf'))

-- Nécessaire pour gen_salt/crypt (et souvent déjà présent si gen_random_uuid() est utilisé)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- SOURCES (ids fixes)
-- =========================
INSERT INTO sources (id, libelle)
VALUES
  (1, 'web'),
  (2, 'mobile')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- STATUS (ids fixes)
-- =========================
INSERT INTO status (id, status_id, libelle)
VALUES
  (1, 'ACTIVE', 'Actif'),
  (2, 'INACTIVE', 'Inactif')
ON CONFLICT (id) DO NOTHING;

-- =========================
-- ROLES (ids fixes)
-- =========================
INSERT INTO roles (id, libelle, niveau)
VALUES
  (1, 'manager', 10),
  (2, 'user', 1)
ON CONFLICT (id) DO NOTHING;

-- =========================
-- UTILISATEURS
-- Mot de passe en clair: password123
-- Hash BCrypt généré en base: crypt('password123', gen_salt('bf', 10))
-- =========================

-- Utilisateur manager
INSERT INTO utilisateurs (email, num_tel, password, nom, prenom, tentatives, id_source, id_status, delete_le)
SELECT
  'manager@example.com',
  '+261330000001',
  crypt('password123', gen_salt('bf', 10)),
  'Manager',
  'Alice',
  0,
  1,
  1,
  NULL
WHERE NOT EXISTS (SELECT 1 FROM utilisateurs WHERE email = 'manager@example.com');

-- Utilisateur normal
INSERT INTO utilisateurs (email, num_tel, password, nom, prenom, tentatives, id_source, id_status, delete_le)
SELECT
  'test@example.com',
  '+261340000000',
  crypt('password123', gen_salt('bf', 10)),
  'Doe',
  'John',
  0,
  1,
  1,
  NULL
WHERE NOT EXISTS (SELECT 1 FROM utilisateurs WHERE email = 'test@example.com');

-- Un utilisateur bloqué (tentatives >= 3) pour tester l'écran /users
INSERT INTO utilisateurs (email, num_tel, password, nom, prenom, tentatives, id_source, id_status, delete_le)
SELECT
  'blocked@example.com',
  '+261330000002',
  crypt('password123', gen_salt('bf', 10)),
  'Blocked',
  'Bob',
  3,
  1,
  1,
  NULL
WHERE NOT EXISTS (SELECT 1 FROM utilisateurs WHERE email = 'blocked@example.com');

-- =========================
-- USERS_ROLES
-- =========================

-- manager@example.com -> role manager
INSERT INTO users_roles (id_utilisateur, id_role)
SELECT u.id, 1
FROM utilisateurs u
WHERE u.email = 'manager@example.com'
  AND NOT EXISTS (
    SELECT 1 FROM users_roles ur WHERE ur.id_utilisateur = u.id AND ur.id_role = 1
  );

-- test@example.com -> role user
INSERT INTO users_roles (id_utilisateur, id_role)
SELECT u.id, 2
FROM utilisateurs u
WHERE u.email = 'test@example.com'
  AND NOT EXISTS (
    SELECT 1 FROM users_roles ur WHERE ur.id_utilisateur = u.id AND ur.id_role = 2
  );

-- blocked@example.com -> role user
INSERT INTO users_roles (id_utilisateur, id_role)
SELECT u.id, 2
FROM utilisateurs u
WHERE u.email = 'blocked@example.com'
  AND NOT EXISTS (
    SELECT 1 FROM users_roles ur WHERE ur.id_utilisateur = u.id AND ur.id_role = 2
  );

-- =========================
-- Forcer delete_le à NULL si tu avais soft-delete quelqu'un
-- =========================
UPDATE utilisateurs
SET delete_le = NULL
WHERE email IN ('test@example.com', 'manager@example.com', 'blocked@example.com');
