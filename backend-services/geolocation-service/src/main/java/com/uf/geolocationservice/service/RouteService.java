package com.uf.geolocationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Service pour obtenir des routes réelles basées sur OpenStreetMap via OSRM.
 * OSRM (Open Source Routing Machine) calcule les itinéraires en suivant les routes réelles de la ville.
 */
@Service
public class RouteService {
	private static final Logger log = LoggerFactory.getLogger(RouteService.class);
	
	// URL du service OSRM (utilise le service public par défaut)
	@Value("${routing.osrm.url:https://router.project-osrm.org/route/v1/driving}")
	private String osrmUrl;
	
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	
	public RouteService(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		this.objectMapper = new ObjectMapper();
	}
	
	/**
	 * Obtient une route réelle entre deux points en utilisant OSRM.
	 * La route suit les routes réelles de la ville (boulevards, avenues, rues).
	 * 
	 * @param fromLat Latitude du point de départ
	 * @param fromLon Longitude du point de départ
	 * @param toLat Latitude du point d'arrivée
	 * @param toLon Longitude du point d'arrivée
	 * @return Liste des points [lat, lon] formant la route réelle
	 */
	public List<double[]> getRoute(double fromLon, double fromLat, double toLon, double toLat) {
		try {
			// Format OSRM: /route/v1/{profile}/{coordinates}?overview=full&geometries=geojson
			// coordinates: lon1,lat1;lon2,lat2
			String coordinates = String.format("%.6f,%.6f;%.6f,%.6f", fromLon, fromLat, toLon, toLat);
			String url = String.format("%s/%s?overview=full&geometries=geojson", osrmUrl, coordinates);
			
			log.debug("Requesting route from OSRM: {}", url);
			
			String response = restTemplate.getForObject(url, String.class);
			
			if (response == null) {
				log.warn("OSRM returned null response");
				return getFallbackRoute(fromLat, fromLon, toLat, toLon);
			}
			
			JsonNode root = objectMapper.readTree(response);
			
			// Vérifier le code de statut
			String code = root.path("code").asText();
			if (!"Ok".equals(code)) {
				log.warn("OSRM returned error code: {}", code);
				return getFallbackRoute(fromLat, fromLon, toLat, toLon);
			}
			
			// Extraire les coordonnées de la route
			JsonNode routes = root.path("routes");
			if (!routes.isArray() || routes.size() == 0) {
				log.warn("No routes found in OSRM response");
				return getFallbackRoute(fromLat, fromLon, toLat, toLon);
			}
			
			JsonNode route = routes.get(0);
			JsonNode geometry = route.path("geometry");
			JsonNode coordinatesArray = geometry.path("coordinates");
			
			List<double[]> routePoints = new ArrayList<>();
			
			// OSRM retourne [lon, lat] dans GeoJSON, on convertit en [lat, lon]
			for (JsonNode coord : coordinatesArray) {
				if (coord.isArray() && coord.size() >= 2) {
					double lon = coord.get(0).asDouble();
					double lat = coord.get(1).asDouble();
					routePoints.add(new double[]{lat, lon});
				}
			}
			
			if (routePoints.isEmpty()) {
				log.warn("No coordinates found in route geometry");
				return getFallbackRoute(fromLat, fromLon, toLat, toLon);
			}
			
			log.info("Retrieved route with {} points from OSRM", routePoints.size());
			return routePoints;
			
		} catch (Exception e) {
			log.error("Error fetching route from OSRM, using fallback", e);
			return getFallbackRoute(fromLat, fromLon, toLat, toLon);
		}
	}
	
	/**
	 * Route de secours (ligne droite) si OSRM n'est pas disponible.
	 */
	private List<double[]> getFallbackRoute(double fromLat, double fromLon, double toLat, double toLon) {
		List<double[]> fallback = new ArrayList<>();
		fallback.add(new double[]{fromLat, fromLon});
		fallback.add(new double[]{toLat, toLon});
		return fallback;
	}
	
	/**
	 * Calcule la distance totale d'une route en mètres.
	 */
	public double calculateRouteDistance(List<double[]> route) {
		if (route.size() < 2) {
			return 0.0;
		}
		
		double totalDistance = 0.0;
		for (int i = 1; i < route.size(); i++) {
			double[] prev = route.get(i - 1);
			double[] curr = route.get(i);
			totalDistance += haversineDistance(prev[0], prev[1], curr[0], curr[1]);
		}
		return totalDistance;
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


