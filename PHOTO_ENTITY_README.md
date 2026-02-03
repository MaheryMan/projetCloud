# Entité Photo - Documentation

## Vue d'ensemble

L'entité `Photo` a été créée pour séparer la gestion des photos des signalements. **Un signalement peut maintenant avoir plusieurs photos** (relation `@OneToMany`), permettant de capturer différents angles, détails, ou états (avant/après) d'un même problème routier.

## Structure de l'entité

### Table `photos`

```sql
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
    -- Clé étrangère vers signalement (relation @ManyToOne)
    id_signalement BIGINT REFERENCES signalements(id) ON DELETE CASCADE
);
```

### Relation avec `signalements`

**Relation OneToMany** : Un signalement peut avoir plusieurs photos. Les photos font référence au signalement via `id_signalement`.

## Classes Java

### Signalement.java (modifié)
- Relation `@OneToMany` avec `List<Photo> photos`
- Méthodes `addPhoto()`, `removePhoto()` pour gérer la collection
- Méthode `getPhotoUrl()` retourne la première photo (compatibilité)
- Méthode `getPhotoUrls()` retourne toutes les URLs

### Photo.java
- Entité JPA avec relation `@ManyToOne` vers Signalement
- Contient métadonnées (taille, type MIME, nom de fichier)
- Horodatage automatique pour création et mise à jour

### PhotoRepository.java
- Interface repository avec méthodes de recherche personnalisées
- Recherche par URL, nom de fichier, type MIME, description

### PhotoService.java
- Service métier pour les opérations sur les photos
- Méthode `findOrCreateByUrl()` pour éviter les doublons
- Gestion des erreurs et logging

### PhotoController.java
- API REST pour gérer les photos
- Endpoints CRUD complets
- Recherche et statistiques

## Migration

### Script SQL
Le script `sql/migration_photos.sql` contient:
- Création de la table `photos`
- Ajout de la colonne `id_photo` dans `signalements`
- Index pour optimiser les performances
- Triggers pour `updated_at`

### Service de migration
`PhotoMigrationService.java` peut être utilisé pour migrer les données existantes si nécessaire.

## API Endpoints

### Photos
- `GET /api/photos` - Liste toutes les photos
- `GET /api/photos/{id}` - Récupère une photo par ID
- `GET /api/photos/by-url?url=...` - Recherche par URL
- `POST /api/photos` - Crée une nouvelle photo
- `PUT /api/photos/{id}` - Met à jour une photo
- `DELETE /api/photos/{id}` - Supprime une photo
- `GET /api/photos/search?keyword=...` - Recherche par description
- `GET /api/photos/stats` - Statistiques des photos
- `GET /api/photos/signalement/{id}` - Photos d'un signalement
- `GET /api/photos/signalement/{id}/count` - Nombre de photos d'un signalement

### Signalements (modifié)
Les endpoints existants des signalements continuent de fonctionner. La gestion de la photo se fait automatiquement via le champ `photoUrl` dans les requêtes.

## Utilisation

### Créer un signalement avec plusieurs photos

```json
POST /api/signalements
{
    "latitude": -18.909855,
    "longitude": 47.525637,
    "description": "Nid de poule",
    "surfaceM2": 5.0,
    "photoUrls": [
        "https://example.com/angle1.jpg",
        "https://example.com/angle2.jpg", 
        "https://example.com/detail.jpg"
    ],
    "idTypeSignalement": 1
}
```

### Créer un signalement avec une seule photo (rétrocompatibilité)

```json
POST /api/signalements
{
    "latitude": -18.909855,
    "longitude": 47.525637,
    "description": "Nid de poule",
    "surfaceM2": 5.0,
    "photoUrl": "https://example.com/photo.jpg",
    "idTypeSignalement": 1
}
```

Le service créera automatiquement l'entité Photo si l'URL n'existe pas déjà.

### Mettre à jour les photos d'un signalement

```json
PUT /api/signalements/{id}
{
    "photoUrls": [
        "https://example.com/nouvelle1.jpg",
        "https://example.com/nouvelle2.jpg"
    ]
}
```

### Supprimer toutes les photos d'un signalement

```json
PUT /api/signalements/{id}
{
    "photoUrl": ""
}
```

## Avantages de cette approche

1. **Support multi-photos** : Un signalement peut avoir plusieurs photos
2. **Évite la duplication** : Plusieurs signalements peuvent partager la même photo
3. **Métadonnées enrichies** : Taille de fichier, type MIME, nom original
4. **Traçabilité** : Horodatage de création et modification
5. **Performance** : Index optimisés pour les recherches
6. **Flexibilité** : Possibilité d'ajouter de nouveaux champs facilement
7. **Intégrité** : Relations de base de données correctes
8. **Rétrocompatibilité** : Les API existantes continuent de fonctionner

## Tests

Les tests d'intégration dans `PhotoIntegrationTest.java` couvrent:
- Création de photos
- Recherche par URL avec évitement de doublons
- Mise à jour et suppression
- Recherche par description
- **Gestion de plusieurs photos par signalement**
- **Récupération des photos d'un signalement spécifique**

## Synchronisation Firebase

Le service `FirebaseSignalementService` a été mis à jour pour:
- Créer automatiquement des entités Photo lors de la synchronisation depuis Firebase
- **Gérer les photos multiples** (champ `photos` array en plus de `photo`)
- Envoyer l'URL de la première photo vers Firebase (compatibilité)
- **Envoyer toutes les photos vers Firebase** dans un array `photos`
- Gérer les erreurs de création de photo sans interrompre la synchronisation

## Notes importantes

1. **Migration en douceur** : L'ancien champ `photoUrl` peut coexister temporairement
2. **Gestion d'erreurs** : Si la création d'une photo échoue, le signalement est quand même créé
3. **Performance** : La relation est `LAZY` pour éviter de charger les photos inutilement
4. **Extensibilité** : Structure prête pour ajouter d'autres types de médias (vidéos, documents)