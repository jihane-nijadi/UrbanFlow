package com.bus.route.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Route entity representing a bus route.
 * One route has many schedules (one-to-many relationship).
 */
@Entity
@Table(name = "routes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String routeId;      // e.g., "R10"
    
    @Column(nullable = false)
    private String routeName;    // e.g., "Maarif - Ain Sebaa"

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Schedule> schedules = new ArrayList<>();

    // Helper methods for managing bidirectional relationship
    public void addSchedule(Schedule schedule) {
        schedules.add(schedule);
        schedule.setRoute(this);
    }

    public void removeSchedule(Schedule schedule) {
        schedules.remove(schedule);
        schedule.setRoute(null);
    }
}
