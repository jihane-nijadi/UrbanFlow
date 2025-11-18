package com.uf.geolocationservice.service;

import com.uf.geolocationservice.model.BusUpdate;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Générateur de données mock pour les bus.
 * Utilise OSRM pour obtenir des routes réelles basées sur OpenStreetMap.
 * Les bus se déplacent le long des routes réelles de la ville (boulevards, avenues, rues).
 */
@Component
public class MockDataGenerator {
	private static final Logger log = LoggerFactory.getLogger(MockDataGenerator.class);

	@Value("${external.api.enable-mock:false}")
	private boolean enableMock;
	
	/**
	 * Vitesse de base des bus en km/h.
	 * Modifiez cette valeur pour changer la vitesse de déplacement.
	 * Valeur recommandée: 30-50 km/h pour un mouvement réaliste.
	 */
	@Value("${mock.bus.speed-kmh:40}")
	private double busSpeedKmh;
	
	/**
	 * Intervalle de mise à jour en millisecondes.
	 * Plus petit = mouvement plus fluide mais plus de requêtes.
	 */
	@Value("${mock.bus.update-interval-ms:2000}")
	private long updateIntervalMs;

	private final List<MockBus> mockBuses = new ArrayList<>();
	private final RouteService routeService;
	private final BusStopService busStopService;

	public MockDataGenerator(RouteService routeService, BusStopService busStopService) {
		this.routeService = routeService;
		this.busStopService = busStopService;
	}

	@PostConstruct
	public void initialize() {
		if (enableMock) {
			log.info("Initializing mock buses with speed: {} km/h, update interval: {} ms", 
					busSpeedKmh, updateIntervalMs);
			initializeMockBuses();
		}
	}

	/**
	 * Initialise 20 bus dispersés dans différentes zones de Casablanca.
	 * Utilise les arrêts réels du BusStopService pour créer des routes réalistes.
	 * Chaque bus a un terminus de départ, des arrêts intermédiaires, et un terminus d'arrivée.
	 * Les bus sont placés dans des zones réelles de la ville, en évitant la mer (longitude > -7.60).
	 * Les routes suivent uniquement les routes réelles via OSRM.
	 */
	private void initializeMockBuses() {
		// Récupérer tous les arrêts réels de Casablanca
		List<com.uf.geolocationservice.model.BusStop> allStops = busStopService.getAllStops();
		
		if (allStops.isEmpty()) {
			log.warn("No bus stops available, using fallback zones");
			initializeMockBusesWithFallback();
			return;
		}
		
		// Séparer les arrêts par type
		List<com.uf.geolocationservice.model.BusStop> departureStops = allStops.stream()
			.filter(s -> "departure".equals(s.getType()))
			.toList();
		
		List<com.uf.geolocationservice.model.BusStop> arrivalStops = allStops.stream()
			.filter(s -> "arrival".equals(s.getType()))
			.toList();
		
		List<com.uf.geolocationservice.model.BusStop> intermediateStops = allStops.stream()
			.filter(s -> "intermediate".equals(s.getType()))
			.toList();
		
		// Créer 20 bus en utilisant les arrêts réels
		for (int i = 0; i < 20; i++) {
			String busId = String.format("BUS-%03d", i + 1);
			String lineId = "LINE-" + (i % 3 + 1); // Répartir sur 3 lignes
			
			// Sélectionner un terminus de départ (parmi les arrêts de départ)
			com.uf.geolocationservice.model.BusStop departureStop;
			if (!departureStops.isEmpty()) {
				departureStop = departureStops.get(i % departureStops.size());
			} else {
				// Fallback si pas d'arrêts de départ
				departureStop = allStops.get(i % allStops.size());
			}
			
			// Sélectionner un terminus d'arrivée (différent du départ)
			com.uf.geolocationservice.model.BusStop arrivalStop;
			if (!arrivalStops.isEmpty()) {
				int arrivalIndex = (i + 10) % arrivalStops.size();
				arrivalStop = arrivalStops.get(arrivalIndex);
			} else {
				// Fallback: utiliser un arrêt éloigné
				int arrivalIndex = (i + allStops.size() / 2) % allStops.size();
				arrivalStop = allStops.get(arrivalIndex);
			}
			
			// S'assurer que l'arrivée est différente du départ
			if (arrivalStop.getStopId().equals(departureStop.getStopId())) {
				int newIndex = (i + 15) % allStops.size();
				arrivalStop = allStops.get(newIndex);
			}
			
			// Vérifier que les coordonnées ne sont pas dans la mer
			double departureLat = departureStop.getLat();
			double departureLon = Math.max(departureStop.getLon(), -7.60); // Protection mer
			
			double arrivalLat = arrivalStop.getLat();
			double arrivalLon = Math.max(arrivalStop.getLon(), -7.60); // Protection mer
			
			// Sélectionner 2-3 arrêts intermédiaires réels sur la même ligne
			List<double[]> selectedIntermediateStops = new ArrayList<>();
			int numIntermediate = 2 + (int)(Math.random() * 2); // 2 ou 3 arrêts
			
			// Filtrer les arrêts intermédiaires qui sont sur la même ligne
			List<com.uf.geolocationservice.model.BusStop> lineIntermediateStops = intermediateStops.stream()
				.filter(s -> s.getLineIds() != null && s.getLineIds().contains(lineId))
				.toList();
			
			// Sélectionner des arrêts intermédiaires entre départ et arrivée
			for (int j = 0; j < numIntermediate && j < lineIntermediateStops.size(); j++) {
				int stopIndex = (i * numIntermediate + j) % lineIntermediateStops.size();
				com.uf.geolocationservice.model.BusStop interStop = lineIntermediateStops.get(stopIndex);
				
				// Vérifier que l'arrêt n'est pas le départ ou l'arrivée
				if (!interStop.getStopId().equals(departureStop.getStopId()) &&
					!interStop.getStopId().equals(arrivalStop.getStopId())) {
					
					double interLat = interStop.getLat();
					double interLon = Math.max(interStop.getLon(), -7.60); // Protection mer
					selectedIntermediateStops.add(new double[]{interLat, interLon});
				}
			}
			
			// Si pas assez d'arrêts intermédiaires, en créer entre départ et arrivée
			while (selectedIntermediateStops.size() < numIntermediate) {
				double ratio = (double)(selectedIntermediateStops.size() + 1) / (numIntermediate + 1);
				double interLat = departureLat + (arrivalLat - departureLat) * ratio;
				double interLon = departureLon + (arrivalLon - departureLon) * ratio;
				
				// Protection mer
				interLon = Math.max(interLon, -7.60);
				
				selectedIntermediateStops.add(new double[]{interLat, interLon});
			}
			
			// Vitesse individuelle (33-52 km/h)
			double speedKmh = 33.0 + Math.random() * 19.0;
			
			// Créer le bus avec les arrêts réels
			createBusWithMultipleStops(busId, lineId, departureLat, departureLon, 
				selectedIntermediateStops, arrivalLat, arrivalLon, speedKmh);
			
			log.info("Created bus {} (Line {}) from {} to {} with {} intermediate stops", 
				busId, lineId, departureStop.getName(), arrivalStop.getName(), 
				selectedIntermediateStops.size());
		}
	}
	
	/**
	 * Méthode de secours si aucun arrêt n'est disponible.
	 * Utilise des zones par défaut.
	 */
	private void initializeMockBusesWithFallback() {
		// Zones de secours (même code qu'avant)
		double[][] zones = {
			{33.5731, -7.5898}, {33.5950, -7.6150}, {33.5680, -7.6000},
			{33.5780, -7.6050}, {33.6080, -7.6300}, {33.3675, -7.5894},
			{33.5900, -7.6000}, {33.5500, -7.5800}, {33.5700, -7.5500},
			{33.5900, -7.5700}, {33.5650, -7.5950}, {33.5800, -7.6000},
			{33.5600, -7.5850}, {33.5850, -7.6100}, {33.5820, -7.5980},
			{33.5550, -7.5750}, {33.5760, -7.5920}, {33.6000, -7.6200},
			{33.5400, -7.5900}, {33.6050, -7.6250}
		};
		
		for (int i = 0; i < 20; i++) {
			String busId = String.format("BUS-%03d", i + 1);
			String lineId = "LINE-" + (i % 3 + 1);
			
			double[] departureZone = zones[i];
			double departureLat = departureZone[0];
			double departureLon = Math.max(departureZone[1], -7.60);
			
			int arrivalIndex = (i + 10) % zones.length;
			double[] arrivalZone = zones[arrivalIndex];
			double arrivalLat = arrivalZone[0];
			double arrivalLon = Math.max(arrivalZone[1], -7.60);
			
			List<double[]> intermediateStops = new ArrayList<>();
			int numIntermediate = 2 + (int)(Math.random() * 2);
			
			for (int j = 1; j <= numIntermediate; j++) {
				double ratio = (double) j / (numIntermediate + 1);
				double interLat = departureLat + (arrivalLat - departureLat) * ratio;
				double interLon = Math.max(departureLon + (arrivalLon - departureLon) * ratio, -7.60);
				intermediateStops.add(new double[]{interLat, interLon});
			}
			
			double speedKmh = 33.0 + Math.random() * 19.0;
			createBusWithMultipleStops(busId, lineId, departureLat, departureLon, 
				intermediateStops, arrivalLat, arrivalLon, speedKmh);
		}
	}

	/**
	 * Crée un bus avec plusieurs arrêts (départ, intermédiaires, arrivée).
	 * Construit une route complète en passant par tous les arrêts.
	 * 
	 * @param busId Identifiant du bus
	 * @param lineId Identifiant de la ligne
	 * @param departureLat Latitude du terminus de départ
	 * @param departureLon Longitude du terminus de départ
	 * @param intermediateStops Liste des arrêts intermédiaires [lat, lon]
	 * @param arrivalLat Latitude du terminus d'arrivée
	 * @param arrivalLon Longitude du terminus d'arrivée
	 * @param speedKmh Vitesse individuelle du bus en km/h
	 */
	private void createBusWithMultipleStops(String busId, String lineId,
			double departureLat, double departureLon,
			List<double[]> intermediateStops,
			double arrivalLat, double arrivalLon, double speedKmh) {
		try {
			List<double[]> fullRoute = new ArrayList<>();
			
			// Construire la route complète en passant par tous les arrêts
			double[] currentPoint = new double[]{departureLat, departureLon};
			fullRoute.add(new double[]{currentPoint[0], currentPoint[1]});
			
			// Ajouter les arrêts intermédiaires
			for (double[] stop : intermediateStops) {
				try {
					// Obtenir la route entre le point actuel et l'arrêt suivant
					List<double[]> segment = routeService.getRoute(
						currentPoint[1], currentPoint[0],
						stop[1], stop[0]
					);
					
					if (segment.size() >= 2) {
						// Ajouter tous les points sauf le premier (déjà ajouté)
						for (int i = 1; i < segment.size(); i++) {
							fullRoute.add(segment.get(i));
						}
					} else {
						// Fallback: ligne droite
						fullRoute.add(new double[]{stop[0], stop[1]});
					}
					
					currentPoint = stop;
				} catch (Exception e) {
					log.warn("Error getting route segment for bus {} to intermediate stop, using direct line", busId);
					fullRoute.add(new double[]{stop[0], stop[1]});
					currentPoint = stop;
				}
			}
			
			// Ajouter le terminus d'arrivée
			try {
				List<double[]> finalSegment = routeService.getRoute(
					currentPoint[1], currentPoint[0],
					arrivalLon, arrivalLat
				);
				
				if (finalSegment.size() >= 2) {
					for (int i = 1; i < finalSegment.size(); i++) {
						fullRoute.add(finalSegment.get(i));
					}
				} else {
					fullRoute.add(new double[]{arrivalLat, arrivalLon});
				}
			} catch (Exception e) {
				log.warn("Error getting final route segment for bus {}, using direct line", busId);
				fullRoute.add(new double[]{arrivalLat, arrivalLon});
			}
			
			// Créer la route de retour (inverse)
			List<double[]> returnRoute = new ArrayList<>();
			for (int i = fullRoute.size() - 1; i >= 0; i--) {
				returnRoute.add(fullRoute.get(i));
			}
			
			if (fullRoute.size() < 2) {
				log.warn("Invalid route for bus {}, using fallback", busId);
				fullRoute = List.of(
					new double[]{departureLat, departureLon},
					new double[]{arrivalLat, arrivalLon}
				);
				returnRoute = List.of(
					new double[]{arrivalLat, arrivalLon},
					new double[]{departureLat, departureLon}
				);
			}
			
			MockBus bus = new MockBus(busId, lineId, fullRoute, returnRoute, speedKmh);
			mockBuses.add(bus);
			
			log.info("Created bus {} (Line {}) with speed {} km/h, {} intermediate stops, route of {} points", 
					busId, lineId, speedKmh, intermediateStops.size(), fullRoute.size());
			
		} catch (Exception e) {
			log.error("Error creating bus {} with multiple stops", busId, e);
			// Créer un bus avec route de secours simple
			List<double[]> fallbackRoute = List.of(
				new double[]{departureLat, departureLon},
				new double[]{arrivalLat, arrivalLon}
			);
			mockBuses.add(new MockBus(busId, lineId, fallbackRoute, fallbackRoute, speedKmh));
		}
	}

	/**
	 * Génère les mises à jour de position pour tous les bus.
	 */
	public List<BusUpdate> generateMockUpdates() {
		if (!enableMock) {
			return List.of();
		}

		List<BusUpdate> updates = new ArrayList<>();
		long now = System.currentTimeMillis();

		for (MockBus bus : mockBuses) {
			// Mettre à jour la position du bus
			bus.updatePosition(updateIntervalMs);

			BusUpdate update = new BusUpdate();
			update.setBusId(bus.busId);
			update.setLineId(bus.lineId);
			update.setLat(bus.lat);
			update.setLon(bus.lon);
			update.setSpeed(bus.speed);
			update.setHeading(bus.heading);
			update.setTs(now);

			updates.add(update);
		}

		return updates;
	}

	public boolean isEnabled() {
		return enableMock;
	}

	/**
	 * Représente un bus mock qui se déplace le long d'une route réelle.
	 */
	private static class MockBus {
		final String busId;
		final String lineId;
		
		// Routes aller et retour
		private final List<double[]> forwardRoute;
		private final List<double[]> returnRoute;
		private List<double[]> currentRoute;
		private boolean goingForward = true;
		
		// Position actuelle
		double lat;
		double lon;
		double speed; // en m/s
		int heading;
		
		// État du mouvement
		private int currentSegmentIndex = 0; // Index du segment actuel dans la route
		private double segmentProgress = 0.0; // Progression dans le segment actuel (0.0 à 1.0)
		private final double speedMs; // Vitesse en m/s
		
		MockBus(String busId, String lineId, List<double[]> forwardRoute, 
				List<double[]> returnRoute, double speedKmh) {
			this.busId = busId;
			this.lineId = lineId;
			this.forwardRoute = forwardRoute;
			this.returnRoute = returnRoute;
			this.currentRoute = forwardRoute;
			this.speedMs = (speedKmh * 1000.0) / 3600.0; // Conversion km/h -> m/s
			
			// Position initiale: départ de la route aller
			if (!forwardRoute.isEmpty()) {
				double[] start = forwardRoute.get(0);
				this.lat = start[0];
				this.lon = start[1];
			}
			
			// Calculer le cap initial
			updateHeading();
		}

		/**
		 * Met à jour la position du bus en fonction du temps écoulé.
		 * Utilise une interpolation linéaire pour un mouvement fluide.
		 */
		void updatePosition(long deltaTimeMs) {
			if (currentRoute.size() < 2) {
				return;
			}
			
			// Distance à parcourir pendant ce deltaTime (en mètres)
			double distanceToMove = speedMs * (deltaTimeMs / 1000.0);
			
			// Ajouter une petite variation aléatoire pour plus de réalisme
			distanceToMove *= (0.9 + Math.random() * 0.2); // Variation de ±10%
			
			// Déplacer le bus le long de la route
			while (distanceToMove > 0 && currentSegmentIndex < currentRoute.size() - 1) {
				double[] currentPoint = currentRoute.get(currentSegmentIndex);
				double[] nextPoint = currentRoute.get(currentSegmentIndex + 1);
				
				// Distance du segment actuel
				double segmentDistance = haversineDistance(
					currentPoint[0], currentPoint[1],
					nextPoint[0], nextPoint[1]
				);
				
				// Distance restante dans le segment actuel
				double remainingInSegment = segmentDistance * (1.0 - segmentProgress);
				
				if (distanceToMove >= remainingInSegment) {
					// Passer au segment suivant
					distanceToMove -= remainingInSegment;
					currentSegmentIndex++;
					segmentProgress = 0.0;
					
					// Vérifier si on a atteint la fin de la route
					if (currentSegmentIndex >= currentRoute.size() - 1) {
						// Inverser la direction
						reverseDirection();
						break;
					}
				} else {
					// Reste dans le segment actuel
					segmentProgress += distanceToMove / segmentDistance;
					distanceToMove = 0;
				}
			}
			
			// Calculer la position actuelle par interpolation
			if (currentSegmentIndex < currentRoute.size() - 1) {
				double[] currentPoint = currentRoute.get(currentSegmentIndex);
				double[] nextPoint = currentRoute.get(currentSegmentIndex + 1);
				
				lat = currentPoint[0] + (nextPoint[0] - currentPoint[0]) * segmentProgress;
				lon = currentPoint[1] + (nextPoint[1] - currentPoint[1]) * segmentProgress;
			} else {
				// À la fin de la route
				double[] endPoint = currentRoute.get(currentRoute.size() - 1);
				lat = endPoint[0];
				lon = endPoint[1];
			}
			
			// Mettre à jour la vitesse et le cap
			speed = speedMs * 3.6; // Conversion m/s -> km/h pour l'affichage
			updateHeading();
		}
		
		/**
		 * Inverse la direction du bus (aller -> retour ou retour -> aller).
		 */
		private void reverseDirection() {
			goingForward = !goingForward;
			currentRoute = goingForward ? forwardRoute : returnRoute;
			currentSegmentIndex = 0;
			segmentProgress = 0.0;
		}
		
		/**
		 * Met à jour le cap (heading) du bus en fonction de sa direction.
		 */
		private void updateHeading() {
			if (currentSegmentIndex < currentRoute.size() - 1) {
				double[] currentPoint = currentRoute.get(currentSegmentIndex);
				double[] nextPoint = currentRoute.get(currentSegmentIndex + 1);
				heading = calculateHeading(currentPoint[0], currentPoint[1], 
						nextPoint[0], nextPoint[1]);
			}
		}
		
		/**
		 * Calcule le cap entre deux points en degrés (0-360).
		 */
		private int calculateHeading(double fromLat, double fromLon, double toLat, double toLon) {
			double dLon = Math.toRadians(toLon - fromLon);
			double y = Math.sin(dLon) * Math.cos(Math.toRadians(toLat));
			double x = Math.cos(Math.toRadians(fromLat)) * Math.sin(Math.toRadians(toLat)) -
					   Math.sin(Math.toRadians(fromLat)) * Math.cos(Math.toRadians(toLat)) * 
					   Math.cos(dLon);
			double bearing = Math.toDegrees(Math.atan2(y, x));
			return (int) ((bearing + 360) % 360);
		}
		
		/**
		 * Calcule la distance entre deux points en utilisant la formule de Haversine (en mètres).
		 */
		private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
			final int R = 6371000; // Rayon de la Terre en mètres
			double dLat = Math.toRadians(lat2 - lat1);
			double dLon = Math.toRadians(lon2 - lon1);
			double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
					   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
					   Math.sin(dLon / 2) * Math.sin(dLon / 2);
			double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
			return R * c;
		}
	}
}
