package com.uf.routeservice.service;

import com.uf.routeservice.config.AppConfig;
import com.uf.routeservice.model.BusState;
import com.uf.routeservice.model.BusStop;
import com.uf.routeservice.model.BusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Client pour communiquer avec le service de géolocalisation.
 */
@Service
public class GeolocationServiceClient {
    private static final Logger log = LoggerFactory.getLogger(GeolocationServiceClient.class);
    
    private final RestTemplate restTemplate;
    private final String geolocationServiceUrl;

    public GeolocationServiceClient(RestTemplate restTemplate, AppConfig appConfig) {
        this.restTemplate = restTemplate;
        this.geolocationServiceUrl = appConfig.getGeolocationServiceUrl();
    }

    /**
     * Récupère tous les arrêts de bus.
     */
    public List<BusStop> getAllStops() {
        try {
            String url = geolocationServiceUrl + "/api/stops";
            ResponseEntity<BusStop[]> response = restTemplate.getForEntity(url, BusStop[].class);
            return Arrays.asList(response.getBody());
        } catch (Exception e) {
            log.error("Error fetching bus stops from geolocation service", e);
            return List.of();
        }
    }

    /**
     * Récupère un arrêt par son ID.
     */
    public Optional<BusStop> getStopById(String stopId) {
        try {
            String url = geolocationServiceUrl + "/api/stops/" + stopId;
            ResponseEntity<BusStop> response = restTemplate.getForEntity(url, BusStop.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.error("Error fetching bus stop {} from geolocation service", stopId, e);
            return Optional.empty();
        }
    }

    /**
     * Récupère tous les bus actifs.
     */
    public List<BusState> getAllBuses() {
        try {
            String url = geolocationServiceUrl + "/api/buses";
            ResponseEntity<BusState[]> response = restTemplate.getForEntity(url, BusState[].class);
            return Arrays.asList(response.getBody());
        } catch (Exception e) {
            log.error("Error fetching buses from geolocation service", e);
            return List.of();
        }
    }

    /**
     * Récupère un bus par son ID.
     */
    public Optional<BusState> getBusById(String busId) {
        try {
            String url = geolocationServiceUrl + "/api/buses/" + busId;
            ResponseEntity<BusState> response = restTemplate.getForEntity(url, BusState.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.error("Error fetching bus {} from geolocation service", busId, e);
            return Optional.empty();
        }
    }

    /**
     * Récupère l'historique des positions d'un bus.
     */
    public List<BusUpdate> getBusPositions(String busId, int limit) {
        try {
            String url = geolocationServiceUrl + "/api/buses/" + busId + "/positions?limit=" + limit;
            ResponseEntity<BusUpdate[]> response = restTemplate.getForEntity(url, BusUpdate[].class);
            return Arrays.asList(response.getBody());
        } catch (Exception e) {
            log.error("Error fetching positions for bus {} from geolocation service", busId, e);
            return List.of();
        }
    }
}

