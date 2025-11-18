# Améliorations du Suivi de Bus en Temps Réel

## 📋 Résumé des Modifications

Ce document décrit toutes les améliorations apportées au système de suivi de bus en temps réel, incluant l'utilisation de routes réelles, une icône de bus moderne, et un mouvement plus fluide.

---

## 🗺️ 1. Routes Réelles via OSRM

### Description
Les bus suivent maintenant les **routes réelles de la ville** (boulevards, avenues, rues principales) au lieu de lignes droites. Cela est réalisé grâce à **OSRM (Open Source Routing Machine)**, qui utilise les données d'OpenStreetMap.

### Fichiers Modifiés/Créés

#### `RouteService.java` (NOUVEAU)
- **Localisation**: `backend-services/geolocation-service/src/main/java/com/uf/geolocationservice/service/RouteService.java`
- **Fonction**: Service qui communique avec l'API OSRM pour obtenir des routes réelles entre deux points
- **Méthodes principales**:
  - `getRoute()`: Obtient une route réelle entre deux coordonnées
  - `calculateRouteDistance()`: Calcule la distance totale d'une route
  - `getFallbackRoute()`: Route de secours (ligne droite) si OSRM n'est pas disponible

#### `MockDataGenerator.java` (MODIFIÉ)
- **Changements**:
  - Utilise maintenant `RouteService` pour obtenir des routes réelles
  - Les bus se déplacent le long de ces routes réelles au lieu de lignes droites
  - Support des routes aller et retour

### Configuration

Dans `application.properties`:
```properties
# URL du service OSRM (service public par défaut)
routing.osrm.url=https://router.project-osrm.org/route/v1/driving
```

**Note**: Vous pouvez utiliser votre propre instance OSRM si vous le souhaitez.

---

## 🚌 2. Icône de Bus Moderne

### Description
L'icône rectangulaire bleue a été remplacée par une **icône de bus moderne en SVG** avec:
- Cercle bleu avec bordure blanche
- Icône SVG de bus stylisée (fenêtres, portes, roues)
- Flèche de direction intégrée
- Rotation fluide selon la direction du bus
- Ombre portée pour une meilleure visibilité

### Fichiers Modifiés

#### `BusTracker.jsx` (MODIFIÉ)
- **Fonction `createBusIcon()`**: Créée une nouvelle icône SVG moderne
- **Caractéristiques**:
  - SVG vectoriel (net à toutes les échelles)
  - Rotation automatique selon le `heading` du bus
  - Transition CSS pour une rotation fluide
  - Design moderne et professionnel

### Code de l'Icône

L'icône est créée avec un SVG intégré qui inclut:
- Cercle de fond bleu (#3b82f6)
- Corps du bus blanc avec fenêtres
- Roues noires
- Flèche de direction blanche

---

## ⚡ 3. Mouvement Plus Rapide et Fluide

### Description
Le système de mouvement a été complètement refait pour:
- **Vitesse réglable** facilement dans la configuration
- **Interpolation linéaire** pour un mouvement fluide entre les points de route
- **Mise à jour plus fréquente** pour une animation plus douce
- **Variation de vitesse** réaliste (±10%)

### Fichiers Modifiés

#### `MockDataGenerator.java` (MODIFIÉ)
- **Classe `MockBus`**:
  - Nouveau système de mouvement basé sur les segments de route
  - Interpolation linéaire entre les points de route
  - Calcul de distance avec formule de Haversine
  - Gestion des routes aller/retour

#### `application.properties` (MODIFIÉ)
```properties
# Vitesse des bus en km/h (MODIFIEZ CETTE VALEUR POUR CHANGER LA VITESSE)
mock.bus.speed-kmh=40

# Intervalle de mise à jour en millisecondes
# Plus petit = mouvement plus fluide mais plus de requêtes
mock.bus.update-interval-ms=2000
```

### Comment Modifier la Vitesse

**Pour changer la vitesse des bus**, modifiez simplement la valeur dans `application.properties`:

```properties
# Exemples:
mock.bus.speed-kmh=30  # Plus lent (30 km/h)
mock.bus.speed-kmh=50  # Plus rapide (50 km/h)
mock.bus.speed-kmh=60  # Très rapide (60 km/h)
```

**Pour un mouvement plus fluide**, réduisez l'intervalle de mise à jour:

```properties
mock.bus.update-interval-ms=1000  # Mise à jour toutes les secondes (plus fluide)
mock.bus.update-interval-ms=500   # Mise à jour toutes les 0.5 secondes (très fluide)
```

---

## 🔧 4. Améliorations Techniques

### Interpolation Linéaire
Le bus se déplace maintenant de manière fluide entre les points de route en utilisant une interpolation linéaire. Cela signifie que le bus ne "saute" plus d'un point à l'autre, mais se déplace progressivement.

### Calcul de Distance
Utilisation de la formule de Haversine pour calculer les distances réelles entre les points, garantissant un mouvement réaliste.

### Gestion des Routes Aller/Retour
Les bus font maintenant des allers-retours entre leur gare de départ et d'arrivée, en suivant les routes réelles dans les deux sens.

---

## 📝 Guide d'Utilisation

### 1. Démarrer le Backend

```bash
cd backend-services/geolocation-service
mvn spring-boot:run
```

Le service va:
- Charger les routes réelles via OSRM pour chaque bus
- Générer des mises à jour de position toutes les 2 secondes (configurable)
- Utiliser la vitesse configurée (40 km/h par défaut)

### 2. Démarrer le Frontend

```bash
cd transport-frontend
npm start
```

### 3. Voir les Bus en Action

1. Ouvrez http://localhost:3000
2. Connectez-vous et allez dans "Bus disponibles"
3. Vous verrez:
   - **Routes bleues** suivant les routes réelles de la ville
   - **Icônes de bus modernes** se déplaçant le long des routes
   - **Mouvement fluide** avec rotation selon la direction

---

## 🎨 Personnalisation

### Changer la Vitesse des Bus

Modifiez `application.properties`:
```properties
mock.bus.speed-kmh=50  # Changez cette valeur
```

### Changer la Fréquence de Mise à Jour

```properties
mock.bus.update-interval-ms=1000  # Changez cette valeur
```

### Personnaliser l'Icône de Bus

Modifiez la fonction `createBusIcon()` dans `BusTracker.jsx`:
- Changez les couleurs dans le SVG
- Modifiez la taille (actuellement 32x32)
- Ajustez le design du bus

### Utiliser Votre Propre Instance OSRM

```properties
routing.osrm.url=http://votre-serveur-osrm:5000/route/v1/driving
```

---

## 🐛 Dépannage

### Les bus ne bougent pas
- Vérifiez que `external.api.enable-mock=true` dans `application.properties`
- Vérifiez les logs du backend pour les erreurs OSRM

### Les routes ne sont pas réelles
- Vérifiez votre connexion Internet (OSRM nécessite un accès externe)
- Vérifiez les logs pour les erreurs de connexion OSRM
- Le système utilisera automatiquement une route de secours (ligne droite) si OSRM n'est pas disponible

### Le mouvement n'est pas fluide
- Réduisez `mock.bus.update-interval-ms` (ex: 1000 ou 500)
- Augmentez la vitesse si les bus semblent trop lents

---

## 📊 Architecture

```
Backend:
├── RouteService.java          → Obtient routes réelles via OSRM
├── MockDataGenerator.java     → Génère positions le long des routes
└── ExternalApiPoller.java     → Diffuse les mises à jour

Frontend:
├── BusTracker.jsx             → Affiche carte avec routes et bus
└── createBusIcon()            → Crée icône SVG moderne
```

---

## ✅ Résumé des Améliorations

| Fonctionnalité | Avant | Après |
|---------------|-------|-------|
| Routes | Lignes droites | Routes réelles (OSRM) |
| Icône | Rectangle bleu | Icône SVG moderne |
| Vitesse | Fixe | Réglable dans config |
| Fluidité | Mise à jour toutes les 5s | Mise à jour toutes les 2s (configurable) |
| Mouvement | Saut entre points | Interpolation linéaire fluide |
| Direction | Basique | Rotation fluide selon heading |

---

## 🚀 Prochaines Étapes Possibles

- [ ] Ajouter des arrêts intermédiaires sur les routes
- [ ] Afficher les noms des rues sur la carte
- [ ] Ajouter des animations de transition plus avancées
- [ ] Support de plusieurs types de véhicules (bus, tram, métro)
- [ ] Intégration avec des données de trafic en temps réel

---

**Date de mise à jour**: 2024
**Version**: 2.0


