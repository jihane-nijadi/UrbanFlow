package com.uf.routeservice.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Modèle pour la requête de recherche d'itinéraire.
 */
public class RouteRequest {
    @NotBlank(message = "Le point de départ est requis")
    private String departureStopId;

    @NotBlank(message = "Le point d'arrivée est requis")
    private String arrivalStopId;

    public RouteRequest() {
    }

    public RouteRequest(String departureStopId, String arrivalStopId) {
        this.departureStopId = departureStopId;
        this.arrivalStopId = arrivalStopId;
    }

    public String getDepartureStopId() {
        return departureStopId;
    }

    public void setDepartureStopId(String departureStopId) {
        this.departureStopId = departureStopId;
    }

    public String getArrivalStopId() {
        return arrivalStopId;
    }

    public void setArrivalStopId(String arrivalStopId) {
        this.arrivalStopId = arrivalStopId;
    }
}


