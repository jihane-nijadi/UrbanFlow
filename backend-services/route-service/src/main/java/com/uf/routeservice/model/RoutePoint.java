package com.uf.routeservice.model;

/**
 * Modèle représentant un point sur le trajet d'un bus.
 */
public class RoutePoint {
    private Double lat;
    private Double lon;
    private Integer order; // Ordre dans le trajet

    public RoutePoint() {
    }

    public RoutePoint(Double lat, Double lon, Integer order) {
        this.lat = lat;
        this.lon = lon;
        this.order = order;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}


