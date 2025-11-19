package com.uf.routeservice.controller;

import com.uf.routeservice.model.BusSchedule;
import com.uf.routeservice.model.BusStop;
import com.uf.routeservice.model.RouteRequest;
import com.uf.routeservice.model.RouteResponse;
import com.uf.routeservice.service.GeolocationServiceClient;
import com.uf.routeservice.service.RouteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/routes")
@CrossOrigin(origins = "*")
public class RouteController {
    
    private final RouteService routeService;
    private final GeolocationServiceClient geolocationClient;

    public RouteController(RouteService routeService, GeolocationServiceClient geolocationClient) {
        this.routeService = routeService;
        this.geolocationClient = geolocationClient;
    }

    /**
     * Récupère tous les arrêts de bus disponibles.
     */
    @GetMapping("/stops")
    public ResponseEntity<List<BusStop>> getAllStops() {
        List<BusStop> stops = geolocationClient.getAllStops();
        return ResponseEntity.ok(stops);
    }

    /**
     * Recherche les bus disponibles pour un itinéraire.
     */
    @PostMapping("/search")
    public ResponseEntity<RouteResponse> searchRoutes(@Valid @RequestBody RouteRequest request) {
        RouteResponse response = routeService.findRoutes(
            request.getDepartureStopId(),
            request.getArrivalStopId()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Récupère le trajet complet d'un bus spécifique.
     */
    @GetMapping("/bus/{busId}/route")
    public ResponseEntity<BusSchedule> getBusRoute(
            @PathVariable String busId,
            @RequestParam String departureStopId,
            @RequestParam String arrivalStopId) {
        Optional<BusSchedule> schedule = routeService.getBusRoute(busId, departureStopId, arrivalStopId);
        return schedule.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

