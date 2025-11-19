package com.uf.geolocationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "bus_state")
public class BusStateEntity {

    @Id
    @Column(name = "bus_id", nullable = false)
    private String busId;

    @Column(name = "line_id")
    private String lineId;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lon")
    private Double lon;

    @Column(name = "last_updated_at")
    private Instant lastUpdatedAt;

    public BusStateEntity() {
    }

    public BusStateEntity(String busId, String lineId, Double lat, Double lon, Instant lastUpdatedAt) {
        this.busId = busId;
        this.lineId = lineId;
        this.lat = lat;
        this.lon = lon;
        this.lastUpdatedAt = lastUpdatedAt;
    }

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

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
