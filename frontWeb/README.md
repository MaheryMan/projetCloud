# FrontWeb - Application de Gestion des Travaux Routiers

Application React pour la gestion et le suivi des travaux routiers à Antananarivo.

## 📋 Fonctionnalités

### Pour les visiteurs (sans compte)
- 🗺️ Visualisation de la carte avec les signalements de problèmes routiers
- 📊 Tableau récapitulatif (nombre de signalements, surface totale, budget, avancement)
- 📍 Détails des signalements au survol (date, statut, surface, budget, entreprise)

### Pour les managers (avec compte)
- 📊 **Dashboard** : Vue d'ensemble avec statistiques
- 📝 **Gestion des signalements** : Modifier statuts, surfaces, budgets, entreprises
- 👥 **Gestion des utilisateurs** : Débloquer les comptes, réinitialiser mots de passe
- 🔄 **Synchronisation Firebase** : Import/Export des données vers le cloud

## 🚀 Installation

```bash
cd frontWeb
npm install
```

## ▶️ Démarrage

```bash
npm start
```

L'application sera accessible sur http://localhost:3000

## 📦 Dépendances

- **React 18.2** : Framework UI
- **React Router DOM** : Navigation entre pages
- **Leaflet** : Affichage de cartes interactives
- **React Leaflet** : Intégration Leaflet avec React

## 🗂️ Structure du projet

```
frontWeb/
├── public/
│   └── index.html
├── src/
│   ├── components/          # Composants réutilisables
│   │   ├── Login.js        # Page de connexion
│   │   ├── Register.js     # Page d'inscription
│   │   └── Navbar.js       # Barre de navigation
│   ├── pages/              # Pages de l'application
│   │   ├── VisitorMap.js           # Carte visiteur
│   │   ├── ManagerDashboard.js     # Dashboard manager
│   │   ├── SignalementManagement.js # Gestion signalements
│   │   ├── UserManagement.js       # Gestion utilisateurs
│   │   └── Synchronization.js      # Synchronisation Firebase
│   ├── App.js             # Configuration des routes
│   ├── App.css
│   └── index.js
└── package.json
```

## 🔐 Routes de l'application

### Routes publiques
- `/` - Carte visiteur
- `/login` - Connexion
- `/register` - Inscription

### Routes protégées (Manager)
- `/dashboard` - Tableau de bord
- `/signalements` - Gestion des signalements
- `/users` - Gestion des utilisateurs
- `/sync` - Synchronisation Firebase

## 🔧 Configuration

### API Backend
L'URL de l'API est configurée dans chaque composant : `http://localhost:8080`

### Serveur de cartes
Le serveur OSM Tile doit être accessible sur : `http://localhost:8081`

## 🗺️ Serveur de cartes

Le projet utilise OpenStreetMap Tile Server (configuré dans docker-compose.yml) pour afficher les cartes d'Antananarivo hors ligne.

## 🔄 Synchronisation

L'application permet de :
- **Récupérer** : Importer les signalements depuis Firebase (créés via mobile)
- **Envoyer** : Exporter les données vers Firebase (pour affichage mobile)
- **Synchronisation complète** : Récupérer puis envoyer

## 🎨 Design

- Design moderne avec gradient violet/bleu
- Interface responsive (mobile-friendly)
- Animations et transitions fluides
- Icônes emoji pour meilleure UX

## 🛡️ Sécurité

- Routes protégées avec authentification JWT
- Token stocké dans localStorage
- Redirection automatique si non authentifié

## 📱 Responsive

L'application est entièrement responsive et s'adapte aux écrans :
- Desktop (1400px+)
- Tablet (768px - 1400px)
- Mobile (< 768px)

## 🚧 Prochaines étapes

Pour lancer l'application complète :

1. Démarrer les services Docker :
```bash
docker-compose up -d
```

2. Installer et démarrer le frontend :
```bash
cd frontWeb
npm install
npm start
```

3. L'application sera accessible sur http://localhost:3000

---

**Note** : Assurez-vous que le backend API Java (port 8080), PostgreSQL (port 5432) et le serveur OSM (port 8081) sont en cours d'exécution.
