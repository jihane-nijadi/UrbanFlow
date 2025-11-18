package com.bus.route.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * Schedule entity representing a bus schedule segment.
 * Many schedules belong to one route (many-to-one relationship).
 */
@Entity
@Table(name = "schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "route") // Prevent circular reference in toString
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String routeId; // Denormalized for quick access
    
    @Column(nullable = false)
    private String departureStop;
    
    @Column(nullable = false)
    private String arrivalStop;
    
    @Column(nullable = false)
    private String departureTime; // Format: "HH:mm" e.g., "08:00"
    
    @Column(nullable = false)
    private String arrivalTime;   // Format: "HH:mm" e.g., "08:25"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_db_id", referencedColumnName = "id")
    @JsonIgnore // Prevent circular reference in JSON serialization
    private Route route;
}
