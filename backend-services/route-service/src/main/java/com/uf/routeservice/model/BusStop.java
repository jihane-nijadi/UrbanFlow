package com.uf.routeservice.model;

import java.util.List;

/**
 * Modèle représentant un arrêt de bus.
 * Copie du modèle du geolocation-service pour la communication inter-services.
 */
public class BusStop {
    private String stopId;
    private String name;
    private String address;
    private String description;
    private Double lat;
    private Double lon;
    private List<String> lineIds;
    private String type;
    private String facilities;
    private Integer capacity;

    public BusStop() {
    }

    // Getters and Setters
    public String getStopId() {
        return stopId;
    }

    public void setStopId(String stopId) {
        this.stopId = stopId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public List<String> getLineIds() {
        return lineIds;
    }

    public void setLineIds(List<String> lineIds) {
        this.lineIds = lineIds;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFacilities() {
        return facilities;
    }

    public void setFacilities(String facilities) {
        this.facilities = facilities;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}


