package com.bus.route.config;

import com.bus.route.entity.Route;
import com.bus.route.entity.Schedule;
import com.bus.route.repository.RouteRepository;
import com.bus.route.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes the database with sample route and schedule data.
 * Runs automatically on application startup.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RouteRepository routeRepository;
    private final ScheduleRepository scheduleRepository;

    @Override
    public void run(String... args) {
        log.info("Starting data initialization...");
        
        // Clear existing data
        scheduleRepository.deleteAll();
        routeRepository.deleteAll();

        // ===========================
        // Route R10: Maarif - Ain Sebaa
        // ===========================
        Route r10 = Route.builder()
                .routeId("R10")
                .routeName("Maarif - Ain Sebaa")
                .build();
        routeRepository.save(r10);
        
        // R10 - Forward direction segments
        createSchedule(r10, "Maarif", "Zerktouni", "08:00", "08:10");
        createSchedule(r10, "Zerktouni", "Casa Port", "08:10", "08:25");
        createSchedule(r10, "Casa Port", "Ain Sebaa", "08:25", "08:45");
        createSchedule(r10, "Maarif", "Casa Port", "08:00", "08:25"); // Direct segment
        createSchedule(r10, "Maarif", "Ain Sebaa", "08:00", "08:45"); // Full route
        
        // R10 - Return direction segments
        createSchedule(r10, "Ain Sebaa", "Casa Port", "09:00", "09:20");
        createSchedule(r10, "Casa Port", "Zerktouni", "09:20", "09:35");
        createSchedule(r10, "Zerktouni", "Maarif", "09:35", "09:45");
        createSchedule(r10, "Casa Port", "Maarif", "09:20", "09:45"); // Direct segment
        createSchedule(r10, "Ain Sebaa", "Maarif", "09:00", "09:45"); // Full route
        
        // Additional R10 schedules for different times
        createSchedule(r10, "Maarif", "Ain Sebaa", "10:00", "10:45");
        createSchedule(r10, "Ain Sebaa", "Maarif", "11:00", "11:45");
        createSchedule(r10, "Maarif", "Casa Port", "12:00", "12:25");
        createSchedule(r10, "Casa Port", "Maarif", "13:00", "13:25");

        // ===========================
        // Route R20: Derb Sultan - Place Mohammed V
        // ===========================
        Route r20 = Route.builder()
                .routeId("R20")
                .routeName("Derb Sultan - Place Mohammed V")
                .build();
        routeRepository.save(r20);
        
        // R20 - Forward direction segments
        createSchedule(r20, "Derb Sultan", "Mers Sultan", "08:00", "08:12");
        createSchedule(r20, "Mers Sultan", "Place Mohammed V", "08:12", "08:27");
        createSchedule(r20, "Derb Sultan", "Place Mohammed V", "08:00", "08:27"); // Direct segment
        
        // R20 - Return direction segments
        createSchedule(r20, "Place Mohammed V", "Mers Sultan", "09:00", "09:15");
        createSchedule(r20, "Mers Sultan", "Derb Sultan", "09:15", "09:27");
        createSchedule(r20, "Place Mohammed V", "Derb Sultan", "09:00", "09:27"); // Direct segment
        
        // Additional R20 schedules for different times
        createSchedule(r20, "Derb Sultan", "Place Mohammed V", "10:00", "10:27");
        createSchedule(r20, "Place Mohammed V", "Derb Sultan", "11:00", "11:27");
        createSchedule(r20, "Derb Sultan", "Mers Sultan", "12:00", "12:12");
        createSchedule(r20, "Mers Sultan", "Derb Sultan", "13:00", "13:12");

        // ===========================
        // Route R30: Casablanca Central - Airport
        // ===========================
        Route r30 = Route.builder()
                .routeId("R30")
                .routeName("Casablanca Central - Airport")
                .build();
        routeRepository.save(r30);
        
        createSchedule(r30, "Casablanca Central", "Airport", "06:00", "06:45");
        createSchedule(r30, "Airport", "Casablanca Central", "07:00", "07:45");
        createSchedule(r30, "Casablanca Central", "Airport", "08:00", "08:45");
        createSchedule(r30, "Airport", "Casablanca Central", "09:00", "09:45");
        createSchedule(r30, "Casablanca Central", "Airport", "14:00", "14:45");
        createSchedule(r30, "Airport", "Casablanca Central", "15:00", "15:45");

        log.info("✅ Data initialization completed successfully!");
        log.info("   - {} routes created", routeRepository.count());
        log.info("   - {} schedules created", scheduleRepository.count());
    }

    private void createSchedule(Route route, String from, String to, String depTime, String arrTime) {
        Schedule schedule = Schedule.builder()
                .routeId(route.getRouteId())
                .departureStop(from)
                .arrivalStop(to)
                .departureTime(depTime)
                .arrivalTime(arrTime)
                .route(route)
                .build();
        scheduleRepository.save(schedule);
    }
}
