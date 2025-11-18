package com.uf.routeservice.model;

import java.time.Instant;
import java.util.List;

/**
 * Modèle représentant l'état d'un bus.
 * Copie du modèle du geolocation-service pour la communication inter-services.
 */
public class BusState {
    private String busId;
    private String lineId;
    private Double lat;
    private Double lon;
    private Double speed;
    private Integer heading;
    private Instant lastUpdatedAt;
    private List<BusUpdate> recentPositions;

    public BusState() {
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

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Integer getHeading() {
        return heading;
    }

    public void setHeading(Integer heading) {
        this.heading = heading;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public List<BusUpdate> getRecentPositions() {
        return recentPositions;
    }

    public void setRecentPositions(List<BusUpdate> recentPositions) {
        this.recentPositions = recentPositions;
    }
}


