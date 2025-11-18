package com.bus.route.mapper;

import com.bus.route.dto.RouteDTO;
import com.bus.route.dto.ScheduleDTO;
import com.bus.route.entity.Route;
import com.bus.route.entity.Schedule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between entities and DTOs.
 * Implements the DTO Pattern for clean data transfer.
 */
@Component
public class RouteMapper {

    public RouteDTO toDTO(Route route, List<Schedule> schedules) {
        return RouteDTO.builder()
                .id(route.getId())
                .routeId(route.getRouteId())
                .routeName(route.getRouteName())
                .schedules(schedules.stream()
                        .map(this::toScheduleDTO)
                        .collect(Collectors.toList()))
                .build();
    }

    public RouteDTO toSimpleDTO(Route route) {
        return RouteDTO.builder()
                .id(route.getId())
                .routeId(route.getRouteId())
                .routeName(route.getRouteName())
                .build();
    }

    public ScheduleDTO toScheduleDTO(Schedule schedule) {
        return ScheduleDTO.builder()
                .id(schedule.getId())
                .routeId(schedule.getRouteId())
                .departureStop(schedule.getDepartureStop())
                .arrivalStop(schedule.getArrivalStop())
                .departureTime(schedule.getDepartureTime())
                .arrivalTime(schedule.getArrivalTime())
                .build();
    }
}
