# Système de Prix Forfaitaire Automatique

## 📋 Vue d'ensemble

Le système de prix forfaitaire permet de calculer automatiquement le budget des signalements en fonction de :
- La **surface en m²**
- Le **niveau** du signalement (saisi manuellement)
- Un **prix par mètre carré** configurable

### Formule de calcul
```
Budget = Prix_par_m² × Niveau × Surface
```

## 🗄️ Structure de la base de données

### Table `prix_forfaitaire`

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | BIGSERIAL | Identifiant unique |
| `prix_par_metre_carre` | NUMERIC(15,2) | Prix de base par m² |
| `created_at` | TIMESTAMP | Date de création |
| `deleted_at` | TIMESTAMP | Date de désactivation (soft delete) |

### Table `signalements` (colonne ajoutée)

| Colonne | Type | Description |
|---------|------|-------------|
| `niveau` | INTEGER | Niveau du signalement (1, 2, 3, etc.) |

## 🚀 Installation

### 1. Exécuter la migration SQL

```bash
psql -U votre_utilisateur -d votre_base < sql/add_prix_forfaitaire_table.sql
```

Ou exécuter manuellement le contenu du fichier dans votre outil de gestion de base de données.

### 2. Configurer le prix initial

Le script SQL insère automatiquement un prix par défaut :
- Prix par m² : 100 Ar

Vous pouvez modifier cette valeur via l'API ou le formulaire web.

## 🔌 API Endpoints

### GET /api/prix-forfaitaire
Récupère tous les prix forfaitaires (actifs et désactivés)

**Réponse :**
```json
[
  {
    "id": 1,
    "prixParMetreCarre": 100.00,
    "createdAt": "2026-02-10T10:00:00",
    "deletedAt": null
  }
]
```

### GET /api/prix-forfaitaire/actif
Récupère le prix forfaitaire actuellement actif

**Réponse :**
```json
{
  "id": 1,
  "prixParMetreCarre": 100.00,
  "createdAt": "2026-02-10T10:00:00",
  "deletedAt": null
}
```

### POST /api/prix-forfaitaire
Crée un nouveau prix forfaitaire (désactive automatiquement les anciens)

**Requête :**
```json
{
  "prixParMetreCarre": 150.00
}
```

**Réponse :**
```json
{
  "id": 2,
  "prixParMetreCarre": 150.00,
  "createdAt": "2026-02-10T11:00:00",
  "deletedAt": null
}
```

### PUT /api/prix-forfaitaire
Met à jour le prix forfaitaire actif (crée un nouveau prix et désactive l'ancien)

**Requête :**
```json
{
  "prixParMetreCarre": 120.00
}
```

### POST /api/prix-forfaitaire/calculer-budget
Calcule le budget pour une surface et un niveau d'urgence donnés

**Requête :**
```json
{
  "surface": 50.00,
  "niveauUrgence": 2
}
```

**Réponse :**
```json
{
  "budget": 11250.00,
  "surface": 50.00,
  "niveauUrgence": 2
}
```

### DELETE /api/prix-forfaitaire/{id}
Désactive un prix forfaitaire (soft delete)

## 💻 Utilisation du formulaire web

### Accéder au formulaire
Importez le composant dans votre application React :

```javascript
import PrixForfaitaire from './pages/PrixForfaitaire';

// Dans votre router
<Route path="/prix-forfaitaire" element={<PrixForfaitaire />} />
```

### Fonctionnalités du formulaire
- ✅ Affichage du prix actif
- ✅ Modification du prix par m²
- ✅ Calcul en temps réel d'exemples pour différents niveaux
- ✅ Validation des données
- ✅ Messages de succès/erreur

## 📊 Exemples de calcul

Avec un prix de **100 Ar/m²** et un multiplicateur de **1.5** :

| Surface | Niveau | Calcul | Budget |
|---------|--------|--------|--------|
| 50 m² | 1 | 50 × 100 × 1.5¹ | 7,500 Ar |
| 50 m² | 2 | 50 × 100 × 1.5² | 11,250 Ar |
| 50 m² | 3 | 50 × 100 × 1.5³ :

| Surface | Niveau | Calcul | Budget |
|---------|--------|--------|--------|
| 50 m² | 1 | 100 × 1 × 50 | 5,000 Ar |
| 50 m² | 2 | 100 × 2 × 50 | 10,000 Ar |
| 50 m² | 3 | 100 × 3 × 50 | 15,000 Ar |
| 100 m² | 1 | 100 × 1 × 100 | 10,000 Ar |
| 100 m² | 2 | 100 × 2 × 100 | 20,000 Ar |

## 🔄 Calcul automatique

Le budget est calculé automatiquement lors de :
- **Création d'un signalement** : Si la surface et le niveau sont fournis
- **Mise à jour d'un signalement** : Si la surface ou le niveau change

### Dans le code Java

La logique se trouve dans `SignalementService.save()` :

```java
public Signalement save(Signalement signalement) {
    if (signalement.getSurfaceM2() != null && signalement.getNiveau() != null) {
        
        BigDecimal budgetCalcule = prixForfaitaireService.calculerBudget(
            signalement.getSurfaceM2(), 
            signalement.getNiveau
```

## ⚠️ Gestion des erreurs

### Aucun prix actif configuré
Si aucun prix forfaitaire n'est actif, le système :
- Lance une exception `IllegalStateException` lors du calcul
- Conserve le budget existant lors de la sauvegarde d'un signalement
- Affiche un message d'avertissement dans les logs

**Solution :** Configurez un prix via l'API ou le formulaire web.

### Validation des données
Les validations suivantes sont effectuées :
- Prix par m² > 0
- Multiplicateur > 0
- Surface > 0 (pour le calcul)
- Niveau d'urgence ≥ 0

## 🔧 Configuration avancée

### Modifier la formule de calcul

Si vous souhaitez modifier la formule de calcul, éditez la méthode `calculerBudget()` dans `PrixForfaitaire.java` :

```java
public BigDecimal calculerBudget(BigDecimal surface, Integer niveauUrgence) {
    // Votre formule personnalisée ici
    BigDecimal multiplicateur = multiplicateurNiveau.pow(niveauUrgence);
    return surface.multiply(prixParMetreCarre).multiply(multiplicateur);
}
```

### Historique des prix

Les anciens prix sont conservés dans la base de données avec leur `deleted_at` renseigné. Vous pouvez :
- Consulter l'historique via l'API
- Restaurer un ancien prix en créant un nouveau prix avec les mêmes valeurs
- Analyser l'évolution des prix dans le temps

## 📝 Notes importantes

1. **Un seul prix actif** : Le système maintient un seul prix actif à la fois
2. **Soft delete** : Les anciens prix sont désactivés mais conservés en base
3. **Calcul automatique** : Le budget est recalculé à chaque sauvegarde si les données sont présentes
4. **Niveau d'urgence** : Provient du `TypeSignalement` associé au signalement

## 🧪 Tests
** : Chaque signalement possède son propre niveau (1, 2, 3, etc.)
Pour tester le système :

1. **Créer un prix forfaitaire :**
```bash
curl -X POST http://localhost:8080/api/prix-forfaitaire \
  -H "Content-Type: application/json" \
  -d '{"prixParMetreCarre": 100, "multiplicateurNiveau": 1.5}'
```}'
```

2. **Calculer un budget :**
```bash
curl -X POST http://localhost:8080/api/prix-forfaitaire/calculer-budget \
  -H "Content-Type: application/json" \
  -d '{"surface": 50, "niveauUrgence": 2}'
```

3. **Créer un signalement avec niveau
## 🤝 Support

Pour toute question ou problème :
- Vérifiez que la migration SQL a été exécutée
- Assurez-vous qu'un prix forfaitaire actif existe
- Consultez les logs de l'application pour les messages d'erreur
