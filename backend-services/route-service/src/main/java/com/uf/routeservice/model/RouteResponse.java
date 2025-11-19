package com.uf.routeservice.model;

import java.util.List;

/**
 * Modèle pour la réponse de recherche d'itinéraire.
 */
public class RouteResponse {
    private BusStop departureStop;
    private BusStop arrivalStop;
    private List<BusSchedule> availableBuses;
    private Integer totalBuses;

    public RouteResponse() {
    }

    public RouteResponse(BusStop departureStop, BusStop arrivalStop, List<BusSchedule> availableBuses) {
        this.departureStop = departureStop;
        this.arrivalStop = arrivalStop;
        this.availableBuses = availableBuses;
        this.totalBuses = availableBuses != null ? availableBuses.size() : 0;
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

    public List<BusSchedule> getAvailableBuses() {
        return availableBuses;
    }

    public void setAvailableBuses(List<BusSchedule> availableBuses) {
        this.availableBuses = availableBuses;
        this.totalBuses = availableBuses != null ? availableBuses.size() : 0;
    }

    public Integer getTotalBuses() {
        return totalBuses;
    }

    public void setTotalBuses(Integer totalBuses) {
        this.totalBuses = totalBuses;
    }
}


