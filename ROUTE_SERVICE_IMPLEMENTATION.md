# Route Service - Documentation d'Implémentation

## Vue d'ensemble

Le **route-service** est un microservice Spring Boot qui permet aux utilisateurs de rechercher des itinéraires entre deux arrêts de bus et d'afficher les bus disponibles avec leurs horaires et trajets complets.

## Architecture

### Backend (Spring Boot)
- **Service**: `route-service` (Port 8083)
- **Communication**: REST API avec `geolocation-service` (Port 8091)
- **Fonctionnalités**:
  - Recherche d'itinéraires entre deux arrêts
  - Affichage des bus disponibles pour un itinéraire
  - Calcul des horaires estimés
  - Génération des trajets complets

### Frontend (React)
- **Composant**: `Trajet.jsx`
- **Intégration**: Intégré dans `PassengerDashboard`
- **Fonctionnalités**:
  - Sélection de point de départ et d'arrivée
  - Affichage des bus disponibles avec horaires
  - Visualisation du trajet sur la carte
  - Affichage des arrêts intermédiaires

## Structure du Service

### Modèles

1. **RouteRequest**: Requête de recherche d'itinéraire
   - `departureStopId`: ID de l'arrêt de départ
   - `arrivalStopId`: ID de l'arrêt d'arrivée

2. **RouteResponse**: Réponse de recherche
   - `departureStop`: Arrêt de départ
   - `arrivalStop`: Arrêt d'arrivée
   - `availableBuses`: Liste des bus disponibles
   - `totalBuses`: Nombre total de bus

3. **BusSchedule**: Horaire et trajet d'un bus
   - `busId`: Identifiant du bus
   - `lineId`: Identifiant de la ligne
   - `departureStop`: Arrêt de départ
   - `arrivalStop`: Arrêt d'arrivée
   - `intermediateStops`: Arrêts intermédiaires
   - `estimatedDepartureTime`: Heure de départ estimée
   - `estimatedArrivalTime`: Heure d'arrivée estimée
   - `estimatedDurationMinutes`: Durée estimée en minutes
   - `routePoints`: Points du trajet complet
   - `currentLat`, `currentLon`: Position actuelle
   - `currentSpeed`, `currentHeading`: Vitesse et direction

4. **RoutePoint**: Point sur le trajet
   - `lat`, `lon`: Coordonnées
   - `order`: Ordre dans le trajet

### Services

1. **GeolocationServiceClient**: Client pour communiquer avec `geolocation-service`
   - `getAllStops()`: Récupère tous les arrêts
   - `getStopById()`: Récupère un arrêt par ID
   - `getAllBuses()`: Récupère tous les bus actifs
   - `getBusById()`: Récupère un bus par ID
   - `getBusPositions()`: Récupère l'historique des positions

2. **RouteService**: Service principal de recherche d'itinéraires
   - `findRoutes()`: Trouve les bus disponibles pour un itinéraire
   - `getBusRoute()`: Récupère le trajet complet d'un bus
   - `findCommonLines()`: Trouve les lignes communes entre deux arrêts
   - `createBusSchedule()`: Crée un horaire de bus
   - `calculateEstimatedTimes()`: Calcule les horaires estimés
   - `findIntermediateStops()`: Trouve les arrêts intermédiaires

### Contrôleur

**RouteController**: Endpoints REST
- `GET /api/routes/stops`: Récupère tous les arrêts
- `POST /api/routes/search`: Recherche d'itinéraires
- `GET /api/routes/bus/{busId}/route`: Récupère le trajet d'un bus

## Frontend

### Composant Trajet.jsx

**Fonctionnalités**:
1. **Sélection des arrêts**:
   - Dropdown pour sélectionner le point de départ
   - Dropdown pour sélectionner le point d'arrivée
   - Chargement automatique des arrêts depuis l'API

2. **Recherche d'itinéraires**:
   - Bouton de recherche
   - Validation des sélections
   - Gestion des erreurs

3. **Affichage des bus disponibles**:
   - Liste des bus avec horaires
   - Durée estimée du trajet
   - Nombre d'arrêts intermédiaires
   - Sélection d'un bus pour voir le trajet

4. **Visualisation sur la carte**:
   - Marqueur vert pour le point de départ
   - Marqueur rouge pour le point d'arrivée
   - Marqueurs bleus pour les arrêts intermédiaires
   - Ligne bleue pour le trajet du bus sélectionné
   - Ajustement automatique de la vue

### Service API

**routeService.js**: Client API pour communiquer avec le route-service
- `getAllStops()`: Récupère tous les arrêts
- `searchRoutes()`: Recherche d'itinéraires
- `getBusRoute()`: Récupère le trajet d'un bus

## Configuration

### Backend (`application.properties`)
```properties
spring.application.name=route-service
server.port=8083
geolocation.service.url=http://localhost:8091
```

### Frontend (`routeService.js`)
```javascript
const ROUTE_API_URL = "http://localhost:8083/api/routes";
```

## Utilisation

### 1. Démarrer les services

```bash
# Terminal 1: Démarrer geolocation-service
cd backend-services/geolocation-service
mvn spring-boot:run

# Terminal 2: Démarrer route-service
cd backend-services/route-service
mvn spring-boot:run

# Terminal 3: Démarrer le frontend
cd transport-frontend
npm start
```

### 2. Utiliser l'interface

1. Se connecter en tant que passager
2. Cliquer sur l'onglet "Trajets" dans le menu
3. Sélectionner un point de départ
4. Sélectionner un point d'arrivée
5. Cliquer sur "Rechercher"
6. Consulter la liste des bus disponibles
7. Cliquer sur un bus pour voir son trajet sur la carte

## Flux de données

1. **Chargement des arrêts**:
   ```
   Frontend → route-service → geolocation-service → Frontend
   ```

2. **Recherche d'itinéraires**:
   ```
   Frontend → route-service → geolocation-service (arrêts + bus)
   route-service → Calcul des itinéraires → Frontend
   ```

3. **Affichage du trajet**:
   ```
   Frontend → route-service → geolocation-service (positions)
   route-service → Construction du trajet → Frontend
   ```

## Algorithme de recherche

1. Récupérer les arrêts de départ et d'arrivée
2. Trouver les lignes communes entre les deux arrêts
3. Filtrer les bus actifs qui utilisent ces lignes
4. Pour chaque bus:
   - Récupérer l'historique des positions
   - Construire les points du trajet
   - Calculer les horaires estimés
   - Trouver les arrêts intermédiaires
5. Retourner la liste des bus disponibles

## Calcul des horaires

- **Distance**: Calculée avec la formule de Haversine
- **Vitesse moyenne**: 40 km/h (ou vitesse actuelle du bus)
- **Durée**: `distance / vitesse * 60` (en minutes)
- **Départ**: Heure actuelle
- **Arrivée**: Départ + Durée

## Fichiers créés/modifiés

### Backend
- `route-service/pom.xml`
- `route-service/src/main/resources/application.properties`
- `route-service/src/main/java/com/uf/routeservice/RouteServiceApplication.java`
- `route-service/src/main/java/com/uf/routeservice/model/*.java`
- `route-service/src/main/java/com/uf/routeservice/service/*.java`
- `route-service/src/main/java/com/uf/routeservice/controller/RouteController.java`
- `route-service/src/main/java/com/uf/routeservice/config/AppConfig.java`

### Frontend
- `transport-frontend/src/api/routeService.js`
- `transport-frontend/src/components/Trajet.jsx`
- `transport-frontend/src/pages/PassengerDashboard.jsx` (modifié)

## Prochaines améliorations possibles

1. **Optimisation des trajets**: Utiliser OSRM pour calculer les routes réelles
2. **Cache**: Mettre en cache les résultats de recherche
3. **Notifications**: Notifier l'utilisateur quand un bus approche
4. **Favoris**: Sauvegarder les itinéraires fréquents
5. **Comparaison**: Comparer plusieurs itinéraires
6. **Temps réel**: Mettre à jour les horaires en temps réel

---

**Date**: 2024
**Version**: 1.0

