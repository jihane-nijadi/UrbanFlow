package com.bus.route.service;

import com.bus.route.dto.RouteDTO;
import com.bus.route.dto.RoutePlanResponse;
import com.bus.route.dto.ScheduleDTO;
import com.bus.route.entity.Route;
import com.bus.route.entity.Schedule;
import com.bus.route.exception.ResourceNotFoundException;
import com.bus.route.mapper.RouteMapper;
import com.bus.route.repository.RouteRepository;
import com.bus.route.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer implementing business logic for Route management.
 * Implements Service Layer Pattern for separation of concerns.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteService {

    private final RouteRepository routeRepository;
    private final ScheduleRepository scheduleRepository;
    private final RouteMapper routeMapper;
    private final RestTemplate restTemplate;

    // ============= ROUTE OPERATIONS =============

    /**
     * Get all routes with their schedules
     */
    public List<RouteDTO> getAllRoutes() {
        log.info("Fetching all routes");
        List<Route> routes = routeRepository.findAll();
        
        return routes.stream()
                .map(route -> {
                    List<Schedule> schedules = scheduleRepository.findByRouteId(route.getRouteId());
                    return routeMapper.toDTO(route, schedules);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get route by ID - Used by Ticket Service
     */
    public RouteDTO getRouteById(Long id) {
        log.info("Fetching route with id: {}", id);
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + id));
        
        List<Schedule> schedules = scheduleRepository.findByRouteId(route.getRouteId());
        return routeMapper.toDTO(route, schedules);
    }

    /**
     * Get route by routeId (e.g., "R10")
     */
    public RouteDTO getRouteByRouteId(String routeId) {
        log.info("Fetching route with routeId: {}", routeId);
        Route route = routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with routeId: " + routeId));
        
        List<Schedule> schedules = scheduleRepository.findByRouteId(route.getRouteId());
        return routeMapper.toDTO(route, schedules);
    }

    /**
     * Check if route exists - Used by Ticket Service
     */
    public boolean isRouteAvailable(Long id) {
        log.info("Checking availability of route with id: {}", id);
        return routeRepository.existsById(id);
    }

    // ============= SCHEDULE OPERATIONS =============

    /**
     * Get schedule by ID - Used by Ticket Service
     */
    public ScheduleDTO getScheduleById(Long id) {
        log.info("Fetching schedule with id: {}", id);
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
        
        return routeMapper.toScheduleDTO(schedule);
    }

    /**
     * Get all schedules for a specific route
     */
    public List<ScheduleDTO> getSchedulesByRouteId(String routeId) {
        log.info("Fetching schedules for routeId: {}", routeId);
        
        // Verify route exists
        routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with routeId: " + routeId));
        
        List<Schedule> schedules = scheduleRepository.findByRouteId(routeId);
        return schedules.stream()
                .map(routeMapper::toScheduleDTO)
                .collect(Collectors.toList());
    }

    /**
     * Check if schedule exists - Used by Ticket Service
     */
    public boolean isScheduleAvailable(Long id) {
        log.info("Checking availability of schedule with id: {}", id);
        return scheduleRepository.existsById(id);
    }

    // ============= STOP OPERATIONS =============

    /**
     * Get all unique stops from all schedules
     */
    public List<String> getAllStops() {
        log.info("Fetching all stops");
        Set<String> stops = new HashSet<>();
        
        scheduleRepository.findAll().forEach(schedule -> {
            stops.add(schedule.getDepartureStop());
            stops.add(schedule.getArrivalStop());
        });
        
        List<String> sortedStops = new ArrayList<>(stops);
        Collections.sort(sortedStops);
        return sortedStops;
    }

    /**
     * Search stops by query string (autocomplete support)
     */
    public List<String> searchStops(String query) {
        log.info("Searching stops with query: {}", query);
        Set<String> stops = new HashSet<>();
        String lowerQuery = query.toLowerCase();
        
        scheduleRepository.findAll().forEach(schedule -> {
            if (schedule.getDepartureStop().toLowerCase().contains(lowerQuery)) {
                stops.add(schedule.getDepartureStop());
            }
            if (schedule.getArrivalStop().toLowerCase().contains(lowerQuery)) {
                stops.add(schedule.getArrivalStop());
            }
        });
        
        List<String> sortedStops = new ArrayList<>(stops);
        Collections.sort(sortedStops);
        return sortedStops;
    }

    // ============= ROUTE PLANNING =============

    /**
     * Plan a route from one stop to another.
     * Finds direct routes and optionally integrates with geolocation service for real-time bus tracking.
     */
    public RoutePlanResponse planRoute(String from, String to) {
        log.info("Planning route from {} to {}", from, to);
        
        // Find direct route
        Optional<Schedule> directRoute = scheduleRepository.findAll().stream()
                .filter(s -> s.getDepartureStop().equalsIgnoreCase(from))
                .filter(s -> s.getArrivalStop().equalsIgnoreCase(to))
                .findFirst();
        
        if (directRoute.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format("No direct route found from %s to %s", from, to));
        }
        
        Schedule schedule = directRoute.get();
        
        // Get route details
        Route route = routeRepository.findByRouteId(schedule.getRouteId())
                .orElse(Route.builder()
                        .routeId(schedule.getRouteId())
                        .routeName("Unknown Route")
                        .build());
        
        // Calculate estimated duration
        Integer duration = calculateDuration(schedule.getDepartureTime(), schedule.getArrivalTime());
        
        // Try to fetch available buses from geolocation service (optional)
        List<Map<String, Object>> buses = fetchAvailableBuses(schedule.getRouteId());
        
        return RoutePlanResponse.builder()
                .routeId(schedule.getRouteId())
                .routeName(route.getRouteName())
                .departureStop(schedule.getDepartureStop())
                .arrivalStop(schedule.getArrivalStop())
                .departureTime(schedule.getDepartureTime())
                .arrivalTime(schedule.getArrivalTime())
                .estimatedDurationMinutes(duration)
                .busesAvailable(buses)
                .build();
    }

    // ============= HELPER METHODS =============

    /**
     * Calculate duration between two time strings
     */
    private Integer calculateDuration(String departureTime, String arrivalTime) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime departure = LocalTime.parse(departureTime, formatter);
            LocalTime arrival = LocalTime.parse(arrivalTime, formatter);
            
            // Handle case where arrival is next day (rare for bus routes)
            if (arrival.isBefore(departure)) {
                arrival = arrival.plusHours(24);
            }
            
            return (int) java.time.Duration.between(departure, arrival).toMinutes();
        } catch (Exception e) {
            log.warn("Failed to calculate duration between {} and {}", departureTime, arrivalTime);
            return null;
        }
    }

    /**
     * Fetch available buses from location service within route-service
     * Uses internal endpoint instead of external service
     */
    private List<Map<String, Object>> fetchAvailableBuses(String routeId) {
        try {
            // Call internal location endpoint (now part of route-service)
            String geoUrl = "http://localhost:8081/api/location/route/" + routeId;
            log.debug("Fetching buses from location service: {}", geoUrl);
            
            List<Map<String, Object>> buses = restTemplate.getForObject(geoUrl, List.class);
            return buses != null ? buses : new ArrayList<>();
        } catch (RestClientException e) {
            log.warn("Location service unavailable for route {}: {}", routeId, e.getMessage());
            return new ArrayList<>(); // Graceful degradation
        }
    }
}
