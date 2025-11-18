package com.bus.route.repository;

import com.bus.route.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByRouteId(String routeId);
}
