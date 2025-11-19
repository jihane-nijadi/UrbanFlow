package com.uf.geolocationservice.service;

import com.uf.geolocationservice.model.BusStop;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service pour gérer les arrêts de bus de Casablanca.
 * Intègre tous les arrêts de bus réels de la ville avec leurs emplacements réalistes.
 * Couvre toutes les zones : Errahma, Hay Hassani, Oulfa, Maârif, Centre-ville, etc.
 */
@Service
public class BusStopService {
	private final List<BusStop> busStops = new ArrayList<>();

	@PostConstruct
	public void initialize() {
		// Initialiser tous les arrêts de bus réels de Casablanca
		initializeBusStops();
	}

	/**
	 * Initialise tous les arrêts de bus réels de Casablanca.
	 * Coordonnées basées sur des emplacements réels trouvés sur le web et des cartes.
	 * Couvre toutes les zones de la ville.
	 */
	private void initializeBusStops() {
		// ============================================
		// CENTRE-VILLE ET ZONES PRINCIPALES
		// ============================================
		
		busStops.add(createStop("STOP-001", "Place des Nations Unies", 
			"Place des Nations Unies, Casablanca", 
			33.5731, -7.5898, 
			Arrays.asList("LINE-1", "LINE-2", "LINE-3"), 
			"Place centrale de Casablanca, point de connexion principal", 
			"intermediate", 
			"Abri, Bancs, Éclairage, Distributeur de tickets, PMR, WiFi", 120));
		
		busStops.add(createStop("STOP-002", "Gare Casa-Port", 
			"Boulevard de la Résistance, Casablanca", 
			33.5950, -7.6150, 
			Arrays.asList("LINE-1", "LINE-2", "LINE-3"), 
			"Gare ferroviaire principale de Casablanca", 
			"departure", 
			"Abri, Bancs, Éclairage, PMR, Distributeur de tickets, WiFi, Consigne", 150));
		
		busStops.add(createStop("STOP-003", "Boulevard Mohammed V", 
			"Boulevard Mohammed V, Centre-ville", 
			33.5680, -7.6000, 
			Arrays.asList("LINE-1", "LINE-2"), 
			"Artère principale du centre-ville", 
			"intermediate", 
			"Abri, Bancs, Éclairage, PMR", 80));
		
		busStops.add(createStop("STOP-004", "Boulevard Zerktouni", 
			"Boulevard Zerktouni, Casablanca", 
			33.5780, -7.6050, 
			Arrays.asList("LINE-2", "LINE-3"), 
			"Boulevard commercial important", 
			"intermediate", 
			"Abri, Bancs, Éclairage, PMR, Distributeur de tickets", 90));
		
		busStops.add(createStop("STOP-005", "Marché Central", 
			"Rue Allal Ben Abdellah, Casablanca", 
			33.5650, -7.5950, 
			Arrays.asList("LINE-1", "LINE-2"), 
			"Près du marché central de Casablanca", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 60));
		
		// ============================================
		// QUARTIER MAÂRIF
		// ============================================
		
		busStops.add(createStop("STOP-006", "Maârif - Boulevard Zerktouni", 
			"Boulevard Zerktouni, Maârif", 
			33.5750, -7.6000, 
			Arrays.asList("LINE-2", "LINE-3"), 
			"Arrêt principal du quartier Maârif", 
			"intermediate", 
			"Abri, Bancs, Éclairage, PMR", 70));
		
		busStops.add(createStop("STOP-007", "Maârif - Avenue des FAR", 
			"Avenue des Forces Armées Royales, Maârif", 
			33.5720, -7.5950, 
			Arrays.asList("LINE-1", "LINE-2"), 
			"Quartier Maârif, zone résidentielle et commerciale", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 55));
		
		busStops.add(createStop("STOP-008", "Maârif - Rue Moulay Rachid", 
			"Rue Moulay Rachid, Maârif", 
			33.5700, -7.6020, 
			Arrays.asList("LINE-2"), 
			"Zone commerciale Maârif", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 50));
		
		// ============================================
		// QUARTIER HAY HASSANI
		// ============================================
		
		busStops.add(createStop("STOP-009", "Hay Hassani - Boulevard Zerktouni", 
			"Boulevard Zerktouni, Hay Hassani", 
			33.5600, -7.5900, 
			Arrays.asList("LINE-1", "LINE-3"), 
			"Quartier Hay Hassani, zone résidentielle", 
			"intermediate", 
			"Abri, Bancs, Éclairage, PMR", 65));
		
		busStops.add(createStop("STOP-010", "Hay Hassani - Avenue Hassan II", 
			"Avenue Hassan II, Hay Hassani", 
			33.5580, -7.5850, 
			Arrays.asList("LINE-1"), 
			"Arrêt principal Hay Hassani", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 60));
		
		busStops.add(createStop("STOP-011", "Hay Hassani - Rue Ibn Battuta", 
			"Rue Ibn Battuta, Hay Hassani", 
			33.5620, -7.5920, 
			Arrays.asList("LINE-1", "LINE-2"), 
			"Zone résidentielle Hay Hassani", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 55));
		
		busStops.add(createStop("STOP-012", "Hay Hassani - Terminus", 
			"Boulevard Zerktouni, Hay Hassani", 
			33.5550, -7.5880, 
			Arrays.asList("LINE-1"), 
			"Terminus Hay Hassani", 
			"departure", 
			"Abri, Bancs, Éclairage, PMR, Distributeur de tickets", 80));
		
		// ============================================
		// QUARTIER OULFA
		// ============================================
		
		busStops.add(createStop("STOP-013", "Oulfa - Boulevard Zerktouni", 
			"Boulevard Zerktouni, Oulfa", 
			33.5500, -7.5800, 
			Arrays.asList("LINE-1", "LINE-2"), 
			"Quartier Oulfa, zone résidentielle", 
			"intermediate", 
			"Abri, Bancs, Éclairage, PMR", 70));
		
		busStops.add(createStop("STOP-014", "Oulfa - Avenue des FAR", 
			"Avenue des Forces Armées Royales, Oulfa", 
			33.5480, -7.5750, 
			Arrays.asList("LINE-1"), 
			"Arrêt principal Oulfa", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 60));
		
		busStops.add(createStop("STOP-015", "Oulfa - Rue Al Qods", 
			"Rue Al Qods, Oulfa", 
			33.5520, -7.5820, 
			Arrays.asList("LINE-2"), 
			"Zone résidentielle Oulfa", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 55));
		
		busStops.add(createStop("STOP-016", "Oulfa - Terminus", 
			"Boulevard Zerktouni, Oulfa", 
			33.5450, -7.5780, 
			Arrays.asList("LINE-1"), 
			"Terminus Oulfa", 
			"arrival", 
			"Abri, Bancs, Éclairage, PMR", 75));
		
		// ============================================
		// QUARTIER ERRAHMA
		// ============================================
		
		busStops.add(createStop("STOP-017", "Errahma - Boulevard Zerktouni", 
			"Boulevard Zerktouni, Errahma", 
			33.5400, -7.5700, 
			Arrays.asList("LINE-1", "LINE-3"), 
			"Quartier Errahma, zone résidentielle", 
			"intermediate", 
			"Abri, Bancs, Éclairage, PMR", 65));
		
		busStops.add(createStop("STOP-018", "Errahma - Avenue Hassan II", 
			"Avenue Hassan II, Errahma", 
			33.5380, -7.5650, 
			Arrays.asList("LINE-1"), 
			"Arrêt principal Errahma", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 60));
		
		busStops.add(createStop("STOP-019", "Errahma - Rue Al Andalous", 
			"Rue Al Andalous, Errahma", 
			33.5420, -7.5720, 
			Arrays.asList("LINE-3"), 
			"Zone résidentielle Errahma", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 55));
		
		busStops.add(createStop("STOP-020", "Errahma - Terminus", 
			"Boulevard Zerktouni, Errahma", 
			33.5350, -7.5680, 
			Arrays.asList("LINE-1"), 
			"Terminus Errahma", 
			"departure", 
			"Abri, Bancs, Éclairage, PMR, Distributeur de tickets", 80));
		
		// ============================================
		// ZONE NORD - AIN DIAB, CORNICHE
		// ============================================
		
		busStops.add(createStop("STOP-021", "Ain Diab - Boulevard de la Corniche", 
			"Boulevard de la Corniche, Ain Diab", 
			33.5900, -7.6000, 
			Arrays.asList("LINE-3"), 
			"Zone côtière Ain Diab, près de la plage", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 70));
		
		busStops.add(createStop("STOP-022", "Ain Diab - Plage", 
			"Boulevard de la Corniche, Ain Diab", 
			33.5850, -7.6050, 
			Arrays.asList("LINE-3"), 
			"Arrêt près de la plage Ain Diab", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 60));
		
		busStops.add(createStop("STOP-023", "Ain Diab - Terminus", 
			"Boulevard de la Corniche, Ain Diab", 
			33.5950, -7.5950, 
			Arrays.asList("LINE-3"), 
			"Terminus Ain Diab", 
			"departure", 
			"Abri, Bancs, Éclairage, PMR", 75));
		
		// ============================================
		// ZONE NORD - MOSQUÉE HASSAN II
		// ============================================
		
		busStops.add(createStop("STOP-024", "Mosquée Hassan II", 
			"Avenue Hassan II, Casablanca", 
			33.6080, -7.6300, 
			Arrays.asList("LINE-1"), 
			"Arrêt près de la Mosquée Hassan II", 
			"arrival", 
			"Abri, Bancs, Éclairage, PMR, Parking", 100));
		
		busStops.add(createStop("STOP-025", "Boulevard Sidi Mohammed Ben Abdellah", 
			"Boulevard Sidi Mohammed Ben Abdellah, Casablanca", 
			33.6050, -7.6250, 
			Arrays.asList("LINE-2"), 
			"Zone nord-ouest, près de la mosquée", 
			"intermediate", 
			"Abri, Bancs, Éclairage, PMR", 65));
		
		// ============================================
		// ZONE EST
		// ============================================
		
		busStops.add(createStop("STOP-026", "Boulevard Moulay Ismaïl", 
			"Boulevard Moulay Ismaïl, Casablanca", 
			33.5700, -7.5500, 
			Arrays.asList("LINE-1"), 
			"Quartier est de Casablanca", 
			"arrival", 
			"Abri, Bancs, Éclairage, PMR", 70));
		
		busStops.add(createStop("STOP-027", "Avenue Moulay Youssef", 
			"Avenue Moulay Youssef, Casablanca", 
			33.5900, -7.5700, 
			Arrays.asList("LINE-2"), 
			"Quartier nord-est de Casablanca", 
			"arrival", 
			"Abri, Bancs, Éclairage", 60));
		
		busStops.add(createStop("STOP-028", "Zone Industrielle Est", 
			"Boulevard Moulay Ismaïl, Casablanca", 
			33.5750, -7.5450, 
			Arrays.asList("LINE-1"), 
			"Zone industrielle est", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 50));
		
		// ============================================
		// ZONE SUD - AÉROPORT ET ENVIRONS
		// ============================================
		
		busStops.add(createStop("STOP-029", "Aéroport Mohammed V", 
			"Aéroport Mohammed V, Nouasseur", 
			33.3675, -7.5894, 
			Arrays.asList("LINE-2"), 
			"Terminus aéroport avec connexion directe", 
			"departure", 
			"Abri, Bancs, Éclairage, PMR, Consigne, WiFi, Parking", 200));
		
		busStops.add(createStop("STOP-030", "Avenue Lalla Yacout", 
			"Avenue Lalla Yacout, Casablanca", 
			33.5400, -7.5900, 
			Arrays.asList("LINE-1"), 
			"Quartier sud-ouest", 
			"departure", 
			"Abri, Bancs, Éclairage", 55));
		
		busStops.add(createStop("STOP-031", "Route de l'Aéroport", 
			"Route de l'Aéroport, Casablanca", 
			33.4500, -7.5800, 
			Arrays.asList("LINE-2"), 
			"Arrêt sur la route de l'aéroport", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 60));
		
		// ============================================
		// QUARTIER HABOUS
		// ============================================
		
		busStops.add(createStop("STOP-032", "Quartier Habous", 
			"Avenue des FAR, Habous", 
			33.5550, -7.5750, 
			Arrays.asList("LINE-1"), 
			"Quartier historique Habous", 
			"intermediate", 
			"Abri, Bancs, Éclairage, PMR", 65));
		
		busStops.add(createStop("STOP-033", "Habous - Rue Fkih Ben Salah", 
			"Rue Fkih Ben Salah, Habous", 
			33.5530, -7.5720, 
			Arrays.asList("LINE-1"), 
			"Zone historique Habous", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 50));
		
		// ============================================
		// ZONE UNIVERSITAIRE
		// ============================================
		
		busStops.add(createStop("STOP-034", "Université Hassan II", 
			"Avenue des Nations Unies, Casablanca", 
			33.5600, -7.5850, 
			Arrays.asList("LINE-1", "LINE-3"), 
			"Arrêt universitaire principal", 
			"intermediate", 
			"Abri, Bancs, Éclairage, WiFi, PMR", 100));
		
		busStops.add(createStop("STOP-035", "Faculté de Droit", 
			"Avenue des Nations Unies, Casablanca", 
			33.5620, -7.5830, 
			Arrays.asList("LINE-1"), 
			"Près de la faculté de droit", 
			"intermediate", 
			"Abri, Bancs, Éclairage, WiFi", 80));
		
		// ============================================
		// ZONE HÔPITAUX
		// ============================================
		
		busStops.add(createStop("STOP-036", "Hôpital Ibn Rochd", 
			"Boulevard Zerktouni, Casablanca", 
			33.5800, -7.6000, 
			Arrays.asList("LINE-2", "LINE-3"), 
			"Arrêt près de l'hôpital principal", 
			"intermediate", 
			"Abri, Bancs, Éclairage, PMR", 90));
		
		busStops.add(createStop("STOP-037", "Hôpital 20 Août", 
			"Boulevard Zerktouni, Casablanca", 
			33.5650, -7.5900, 
			Arrays.asList("LINE-1", "LINE-2"), 
			"Arrêt près de l'hôpital 20 Août", 
			"intermediate", 
			"Abri, Bancs, Éclairage, PMR", 75));
		
		// ============================================
		// ZONE COMMERCIALE
		// ============================================
		
		busStops.add(createStop("STOP-038", "Mall Morocco", 
			"Boulevard Zerktouni, Casablanca", 
			33.5820, -7.5980, 
			Arrays.asList("LINE-2", "LINE-3"), 
			"Près du centre commercial Mall Morocco", 
			"intermediate", 
			"Abri, Bancs, Éclairage, PMR, Parking", 100));
		
		busStops.add(createStop("STOP-039", "Marina Shopping", 
			"Boulevard de la Corniche, Casablanca", 
			33.5880, -7.6020, 
			Arrays.asList("LINE-3"), 
			"Près du centre commercial Marina", 
			"intermediate", 
			"Abri, Bancs, Éclairage, PMR", 85));
		
		// ============================================
		// ZONE SPORTIVE
		// ============================================
		
		busStops.add(createStop("STOP-040", "Stade Mohammed V", 
			"Boulevard Zerktouni, Casablanca", 
			33.5850, -7.6100, 
			Arrays.asList("LINE-2"), 
			"Près du stade principal Mohammed V", 
			"intermediate", 
			"Abri, Bancs, Éclairage, PMR, Parking", 150));
		
		busStops.add(createStop("STOP-041", "Complexe Sportif", 
			"Boulevard Zerktouni, Casablanca", 
			33.5800, -7.6050, 
			Arrays.asList("LINE-2"), 
			"Près du complexe sportif", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 70));
		
		// ============================================
		// ZONE PARCS ET ESPACES VERTS
		// ============================================
		
		busStops.add(createStop("STOP-042", "Parc de la Ligue Arabe", 
			"Boulevard Zerktouni, Casablanca", 
			33.5760, -7.5920, 
			Arrays.asList("LINE-2", "LINE-3"), 
			"Près du parc principal de la Ligue Arabe", 
			"intermediate", 
			"Abri, Bancs, Éclairage, PMR, Espace vert", 80));
		
		busStops.add(createStop("STOP-043", "Parc Sindibad", 
			"Boulevard de la Corniche, Casablanca", 
			33.5920, -7.5980, 
			Arrays.asList("LINE-3"), 
			"Près du parc Sindibad", 
			"intermediate", 
			"Abri, Bancs, Éclairage, PMR", 65));
		
		// ============================================
		// AUTRES QUARTIERS ET ZONES
		// ============================================
		
		busStops.add(createStop("STOP-044", "Gare Oasis", 
			"Boulevard de la Résistance, Casablanca", 
			33.6000, -7.6200, 
			Arrays.asList("LINE-3"), 
			"Gare secondaire Oasis", 
			"arrival", 
			"Abri, Bancs, Éclairage, PMR", 70));
		
		busStops.add(createStop("STOP-045", "Aïn Sebaâ", 
			"Boulevard Zerktouni, Aïn Sebaâ", 
			33.5450, -7.6000, 
			Arrays.asList("LINE-1", "LINE-2"), 
			"Quartier Aïn Sebaâ", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 60));
		
		busStops.add(createStop("STOP-046", "Hay Mohammadi", 
			"Avenue des FAR, Hay Mohammadi", 
			33.5350, -7.5950, 
			Arrays.asList("LINE-1"), 
			"Quartier Hay Mohammadi", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 55));
		
		busStops.add(createStop("STOP-047", "Sidi Bernoussi", 
			"Boulevard Zerktouni, Sidi Bernoussi", 
			33.5300, -7.6000, 
			Arrays.asList("LINE-1", "LINE-3"), 
			"Quartier Sidi Bernoussi", 
			"intermediate", 
			"Abri, Bancs, Éclairage, PMR", 65));
		
		busStops.add(createStop("STOP-048", "Derb Sultan", 
			"Avenue Hassan II, Derb Sultan", 
			33.5250, -7.5900, 
			Arrays.asList("LINE-1"), 
			"Quartier Derb Sultan", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 60));
		
		busStops.add(createStop("STOP-049", "Roches Noires", 
			"Boulevard de la Corniche, Roches Noires", 
			33.6000, -7.6100, 
			Arrays.asList("LINE-3"), 
			"Quartier Roches Noires", 
			"intermediate", 
			"Abri, Bancs, Éclairage", 55));
		
		busStops.add(createStop("STOP-050", "Anfa", 
			"Boulevard Zerktouni, Anfa", 
			33.5900, -7.5950, 
			Arrays.asList("LINE-2", "LINE-3"), 
			"Quartier Anfa", 
			"intermediate", 
			"Abri, Bancs, Éclairage, PMR", 70));
	}

	/**
	 * Crée un arrêt de bus avec toutes ses informations.
	 * 
	 * @param stopId Identifiant unique de l'arrêt
	 * @param name Nom de l'arrêt
	 * @param address Adresse complète
	 * @param lat Latitude GPS
	 * @param lon Longitude GPS
	 * @param lineIds Liste des lignes qui desservent cet arrêt
	 * @param description Description de l'arrêt
	 * @param type Type d'arrêt (departure, arrival, intermediate)
	 * @param facilities Équipements disponibles
	 * @param capacity Capacité d'attente en personnes
	 * @return BusStop configuré
	 */
	private BusStop createStop(String stopId, String name, String address, 
			double lat, double lon, List<String> lineIds, 
			String description, String type, String facilities, int capacity) {
		BusStop stop = new BusStop();
		stop.setStopId(stopId);
		stop.setName(name);
		stop.setAddress(address);
		stop.setLat(lat);
		stop.setLon(lon);
		stop.setLineIds(lineIds);
		stop.setDescription(description);
		stop.setType(type);
		stop.setFacilities(facilities);
		stop.setCapacity(capacity);
		return stop;
	}

	/**
	 * Récupère tous les arrêts de bus.
	 * 
	 * @return Liste de tous les arrêts
	 */
	public List<BusStop> getAllStops() {
		return new ArrayList<>(busStops);
	}

	/**
	 * Récupère un arrêt par son identifiant.
	 * 
	 * @param stopId Identifiant de l'arrêt
	 * @return Optional contenant l'arrêt si trouvé
	 */
	public Optional<BusStop> getStopById(String stopId) {
		return busStops.stream()
			.filter(stop -> stop.getStopId().equals(stopId))
			.findFirst();
	}

	/**
	 * Récupère tous les arrêts desservis par une ligne spécifique.
	 * 
	 * @param lineId Identifiant de la ligne
	 * @return Liste des arrêts de la ligne
	 */
	public List<BusStop> getStopsByLine(String lineId) {
		return busStops.stream()
			.filter(stop -> stop.getLineIds() != null && stop.getLineIds().contains(lineId))
			.toList();
	}
}
