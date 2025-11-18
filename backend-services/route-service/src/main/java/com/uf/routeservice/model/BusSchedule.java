package com.uf.routeservice.model;

import java.time.LocalTime;
import java.util.List;

/**
 * Modèle représentant l'horaire et le trajet d'un bus pour un itinéraire.
 */
public class BusSchedule {
    private String busId;
    private String lineId;
    private BusStop departureStop;
    private BusStop arrivalStop;
    private List<BusStop> intermediateStops;
    private LocalTime estimatedDepartureTime;
    private LocalTime estimatedArrivalTime;
    private Integer estimatedDurationMinutes;
    private Double currentLat;
    private Double currentLon;
    private Double currentSpeed;
    private Integer currentHeading;
    private List<RoutePoint> routePoints; // Points du trajet complet

    public BusSchedule() {
    }

    // Getters and Setters
    public String getBusId() {
        return busId;
    }

    public void setBusId(String busId) {
        this.busId = busId;
    }

    public String getLineId() {
        return lineId;
    }

    public void setLineId(String lineId) {
        this.lineId = lineId;
    }

    public BusStop getDepartureStop() {
        return departureStop;
    }

    public void setDepartureStop(BusStop departureStop) {
        this.departureStop = departureStop;
    }

    public BusStop getArrivalStop() {
        return arrivalStop;
    }

    public void setArrivalStop(BusStop arrivalStop) {
        this.arrivalStop = arrivalStop;
    }

    public List<BusStop> getIntermediateStops() {
        return intermediateStops;
    }

    public void setIntermediateStops(List<BusStop> intermediateStops) {
        this.intermediateStops = intermediateStops;
    }

    public LocalTime getEstimatedDepartureTime() {
        return estimatedDepartureTime;
    }

    public void setEstimatedDepartureTime(LocalTime estimatedDepartureTime) {
        this.estimatedDepartureTime = estimatedDepartureTime;
    }

    public LocalTime getEstimatedArrivalTime() {
        return estimatedArrivalTime;
    }

    public void setEstimatedArrivalTime(LocalTime estimatedArrivalTime) {
        this.estimatedArrivalTime = estimatedArrivalTime;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public Double getCurrentLat() {
        return currentLat;
    }

    public void setCurrentLat(Double currentLat) {
        this.currentLat = currentLat;
    }

    public Double getCurrentLon() {
        return currentLon;
    }

    public void setCurrentLon(Double currentLon) {
        this.currentLon = currentLon;
    }

    public Double getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(Double currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public Integer getCurrentHeading() {
        return currentHeading;
    }

    public void setCurrentHeading(Integer currentHeading) {
        this.currentHeading = currentHeading;
    }

    public List<RoutePoint> getRoutePoints() {
        return routePoints;
    }

    public void setRoutePoints(List<RoutePoint> routePoints) {
        this.routePoints = routePoints;
    }
}


