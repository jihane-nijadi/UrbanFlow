package com.uf.routeservice.service;

import com.uf.routeservice.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service pour trouver les bus disponibles pour un itinéraire donné.
 */
@Service
public class RouteService {
    private static final Logger log = LoggerFactory.getLogger(RouteService.class);
    
    private final GeolocationServiceClient geolocationClient;

    public RouteService(GeolocationServiceClient geolocationClient) {
        this.geolocationClient = geolocationClient;
    }

    /**
     * Trouve tous les bus disponibles pour un itinéraire donné.
     * 
     * @param departureStopId ID de l'arrêt de départ
     * @param arrivalStopId ID de l'arrêt d'arrivée
     * @return RouteResponse contenant les bus disponibles
     */
    public RouteResponse findRoutes(String departureStopId, String arrivalStopId) {
        log.info("Finding routes from {} to {}", departureStopId, arrivalStopId);
        
        // Récupérer les arrêts
        Optional<BusStop> departureStopOpt = geolocationClient.getStopById(departureStopId);
        Optional<BusStop> arrivalStopOpt = geolocationClient.getStopById(arrivalStopId);
        
        if (departureStopOpt.isEmpty() || arrivalStopOpt.isEmpty()) {
            log.warn("One or both stops not found: departure={}, arrival={}", departureStopId, arrivalStopId);
            return new RouteResponse(
                departureStopOpt.orElse(null),
                arrivalStopOpt.orElse(null),
                List.of()
            );
        }
        
        BusStop departureStop = departureStopOpt.get();
        BusStop arrivalStop = arrivalStopOpt.get();
        
        // Trouver les lignes communes entre départ et arrivée
        List<String> commonLines = findCommonLines(departureStop, arrivalStop);
        
        if (commonLines.isEmpty()) {
            log.info("No common lines between {} and {}", departureStopId, arrivalStopId);
            return new RouteResponse(departureStop, arrivalStop, List.of());
        }
        
        // Récupérer tous les bus actifs
        List<BusState> allBuses = geolocationClient.getAllBuses();
        
        // Filtrer les bus qui passent par les deux arrêts
        List<BusSchedule> availableBuses = allBuses.stream()
            .filter(bus -> commonLines.contains(bus.getLineId()))
            .map(bus -> createBusSchedule(bus, departureStop, arrivalStop))
            .collect(Collectors.toList());
        
        log.info("Found {} buses for route from {} to {}", availableBuses.size(), departureStopId, arrivalStopId);
        
        return new RouteResponse(departureStop, arrivalStop, availableBuses);
    }

    /**
     * Trouve les lignes communes entre deux arrêts.
     */
    private List<String> findCommonLines(BusStop departure, BusStop arrival) {
        List<String> departureLines = departure.getLineIds() != null ? departure.getLineIds() : List.of();
        List<String> arrivalLines = arrival.getLineIds() != null ? arrival.getLineIds() : List.of();
        
        return departureLines.stream()
            .filter(arrivalLines::contains)
            .collect(Collectors.toList());
    }

    /**
     * Crée un BusSchedule pour un bus donné.
     */
    private BusSchedule createBusSchedule(BusState bus, BusStop departureStop, BusStop arrivalStop) {
        BusSchedule schedule = new BusSchedule();
        schedule.setBusId(bus.getBusId());
        schedule.setLineId(bus.getLineId());
        schedule.setDepartureStop(departureStop);
        schedule.setArrivalStop(arrivalStop);
        
        // Position actuelle du bus
        schedule.setCurrentLat(bus.getLat());
        schedule.setCurrentLon(bus.getLon());
        schedule.setCurrentSpeed(bus.getSpeed());
        schedule.setCurrentHeading(bus.getHeading());
        
        // Récupérer l'historique des positions pour construire le trajet
        List<BusUpdate> positions = geolocationClient.getBusPositions(bus.getBusId(), 100);
        
        // Construire les points du trajet
        List<RoutePoint> routePoints = buildRoutePoints(positions, departureStop, arrivalStop);
        schedule.setRoutePoints(routePoints);
        
        // If the BusState already contains a scheduledDepartureTime (provided by the mock generator),
        // prefer it and set estimated times from it so each bus instance appears as its own schedule.
        try {
            String sched = bus.getScheduledDepartureTime();
            if (sched != null && !sched.isBlank()) {
                java.time.LocalTime dep = java.time.LocalTime.parse(sched);
                schedule.setEstimatedDepartureTime(dep);
                schedule.setEstimatedArrivalTime(dep.plusMinutes(30));
                schedule.setEstimatedDurationMinutes(30);
            } else {
                // Calculer les horaires estimés (simulation basée sur la distance et la vitesse)
                calculateEstimatedTimes(schedule, positions);
            }
        } catch (java.time.format.DateTimeParseException e) {
            // Fall back to distance-based estimate if parsing fails
            calculateEstimatedTimes(schedule, positions);
        }
        
        // Trouver les arrêts intermédiaires
        List<BusStop> intermediateStops = findIntermediateStops(bus.getLineId(), departureStop, arrivalStop);
        schedule.setIntermediateStops(intermediateStops);
        
        return schedule;
    }

    /**
     * Construit les points du trajet à partir de l'historique des positions.
     */
    private List<RoutePoint> buildRoutePoints(List<BusUpdate> positions, BusStop departure, BusStop arrival) {
        List<RoutePoint> routePoints = new ArrayList<>();
        
        // Si on a des positions historiques, les utiliser
        if (!positions.isEmpty()) {
            int order = 0;
            for (BusUpdate position : positions) {
                routePoints.add(new RoutePoint(position.getLat(), position.getLon(), order++));
            }
        } else {
            // Sinon, créer un trajet simple entre départ et arrivée
            routePoints.add(new RoutePoint(departure.getLat(), departure.getLon(), 0));
            routePoints.add(new RoutePoint(arrival.getLat(), arrival.getLon(), 1));
        }
        
        return routePoints;
    }

    /**
     * Calcule les horaires estimés de départ et d'arrivée.
     */
    private void calculateEstimatedTimes(BusSchedule schedule, List<BusUpdate> positions) {
        // Prefer scheduledDepartureTime if provided by geolocation mock data
        try {
            String sched = null;
            // try to get scheduledDepartureTime from route points or other attached metadata
            if (schedule.getRoutePoints() != null && !schedule.getRoutePoints().isEmpty()) {
                // no-op, placeholder for future
            }
            // If the underlying BusState provided a scheduledDepartureTime via geolocation client,
            // it will have been serialized into the BusSchedule.raw fields if available. The GeolocationServiceClient
            // returns BusState which routeService passed to createBusSchedule; we can inspect schedule.getCurrentSpeed()
            // but more reliable is to check the positions list for scheduledDepartureTime in their payload if present.
            if (positions != null && !positions.isEmpty()) {
                // positions[0] may contain a scheduledDepartureTime field when produced by the mock generator
                BusUpdate first = positions.get(0);
                try {
                    java.lang.reflect.Method m = first.getClass().getMethod("getScheduledDepartureTime");
                    Object val = m.invoke(first);
                    if (val instanceof String) sched = (String) val;
                } catch (NoSuchMethodException ignored) {
                    // field not present
                }
            }

            if (sched != null && !sched.isBlank()) {
                // parse sched into LocalTime
                LocalTime dep = LocalTime.parse(sched);
                schedule.setEstimatedDepartureTime(dep);
                schedule.setEstimatedArrivalTime(dep.plusMinutes(30));
                schedule.setEstimatedDurationMinutes(30);
                return;
            }
        } catch (Exception e) {
            log.debug("Could not parse scheduledDepartureTime, falling back to distance-based estimate", e);
        }

        // Fallback: Simulation simple : utiliser l'heure actuelle + estimation basée sur la distance
        LocalTime now = LocalTime.now();
        
        // Distance approximative entre départ et arrivée (en km)
        double distance = calculateDistance(
            schedule.getDepartureStop().getLat(), schedule.getDepartureStop().getLon(),
            schedule.getArrivalStop().getLat(), schedule.getArrivalStop().getLon()
        );
        
        // Vitesse moyenne estimée (40 km/h)
        double avgSpeedKmh = schedule.getCurrentSpeed() != null ? schedule.getCurrentSpeed() : 40.0;
        if (avgSpeedKmh <= 0) avgSpeedKmh = 40.0;
        
        // Durée estimée en minutes
        int durationMinutes = (int) ((distance / avgSpeedKmh) * 60);
        schedule.setEstimatedDurationMinutes(durationMinutes);
        
        // Heure de départ estimée (maintenant)
        schedule.setEstimatedDepartureTime(now);
        
        // Heure d'arrivée estimée
        schedule.setEstimatedArrivalTime(now.plusMinutes(durationMinutes));
    }

    /**
     * Calcule la distance entre deux points en kilomètres (formule de Haversine).
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Rayon de la Terre en km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    /**
     * Trouve les arrêts intermédiaires entre départ et arrivée pour une ligne donnée.
     */
    private List<BusStop> findIntermediateStops(String lineId, BusStop departure, BusStop arrival) {
        // Récupérer tous les arrêts de la ligne
        List<BusStop> allStops = geolocationClient.getAllStops();
        
        List<BusStop> lineStops = allStops.stream()
            .filter(stop -> stop.getLineIds() != null && stop.getLineIds().contains(lineId))
            .filter(stop -> !stop.getStopId().equals(departure.getStopId()) 
                         && !stop.getStopId().equals(arrival.getStopId()))
            .collect(Collectors.toList());
        
        // Trier par distance depuis le départ (approximation simple)
        lineStops.sort((s1, s2) -> {
            double dist1 = calculateDistance(departure.getLat(), departure.getLon(), s1.getLat(), s1.getLon());
            double dist2 = calculateDistance(departure.getLat(), departure.getLon(), s2.getLat(), s2.getLon());
            return Double.compare(dist1, dist2);
        });
        
        // Retourner les arrêts qui sont entre départ et arrivée
        List<BusStop> intermediate = new ArrayList<>();
        for (BusStop stop : lineStops) {
            double distFromDeparture = calculateDistance(departure.getLat(), departure.getLon(), stop.getLat(), stop.getLon());
            double distToArrival = calculateDistance(stop.getLat(), stop.getLon(), arrival.getLat(), arrival.getLon());
            double totalDist = calculateDistance(departure.getLat(), departure.getLon(), arrival.getLat(), arrival.getLon());
            
            // Si l'arrêt est sur le chemin (approximation)
            if (distFromDeparture + distToArrival <= totalDist * 1.2) { // 20% de tolérance
                intermediate.add(stop);
            }
        }
        
        return intermediate;
    }

    /**
     * Récupère le trajet complet d'un bus spécifique.
     */
    public Optional<BusSchedule> getBusRoute(String busId, String departureStopId, String arrivalStopId) {
        Optional<BusState> busOpt = geolocationClient.getBusById(busId);
        Optional<BusStop> departureOpt = geolocationClient.getStopById(departureStopId);
        Optional<BusStop> arrivalOpt = geolocationClient.getStopById(arrivalStopId);
        
        if (busOpt.isEmpty() || departureOpt.isEmpty() || arrivalOpt.isEmpty()) {
            return Optional.empty();
        }
        
        BusSchedule schedule = createBusSchedule(busOpt.get(), departureOpt.get(), arrivalOpt.get());
        return Optional.of(schedule);
    }
}

