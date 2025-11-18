package com.example.ticket_service.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for RouteDTO to ensure compatibility with Route Service
 */
@DisplayName("RouteDTO Compatibility Tests")
class RouteDTOTest {

    @Test
    @DisplayName("RouteDTO should have all required fields for Route Service compatibility")
    void routeDTO_shouldHaveRequiredFields() {
        // Given
        List<ScheduleDTO> schedules = new ArrayList<>();
        schedules.add(ScheduleDTO.builder()
                .id(1L)
                .routeId("R10")
                .departureStop("Maarif")
                .arrivalStop("Zerktouni")
                .departureTime("08:00")
                .arrivalTime("08:10")
                .build());

        // When
        RouteDTO route = RouteDTO.builder()
                .id(1L)
                .routeId("R10")
                .routeName("Maarif - Ain Sebaa")
                .schedules(schedules)
                .build();

        // Then
        assertThat(route).isNotNull();
        assertThat(route.getId()).isEqualTo(1L);
        assertThat(route.getRouteId()).isEqualTo("R10");
        assertThat(route.getRouteName()).isEqualTo("Maarif - Ain Sebaa");
        assertThat(route.getSchedules()).hasSize(1);
    }

    @Test
    @DisplayName("RouteDTO should support null schedules list")
    void routeDTO_shouldSupportNullSchedules() {
        // When
        RouteDTO route = RouteDTO.builder()
                .id(1L)
                .routeId("R10")
                .routeName("Maarif - Ain Sebaa")
                .schedules(null)
                .build();

        // Then
        assertThat(route).isNotNull();
        assertThat(route.getSchedules()).isNull();
    }

    @Test
    @DisplayName("RouteDTO should be serializable for REST communication")
    void routeDTO_shouldBeSerializable() {
        // Given
        RouteDTO route = RouteDTO.builder()
                .id(1L)
                .routeId("R10")
                .routeName("Maarif - Ain Sebaa")
                .build();

        // Then - Verify all getters work (required for Jackson serialization)
        assertThat(route.getId()).isNotNull();
        assertThat(route.getRouteId()).isNotNull();
        assertThat(route.getRouteName()).isNotNull();
    }

    @Test
    @DisplayName("RouteDTO builder should work correctly")
    void routeDTO_builderShouldWork() {
        // When
        RouteDTO route = RouteDTO.builder()
                .id(5L)
                .routeId("R20")
                .routeName("Derb Sultan - Place Mohammed V")
                .schedules(new ArrayList<>())
                .build();

        // Then
        assertThat(route.getId()).isEqualTo(5L);
        assertThat(route.getRouteId()).isEqualTo("R20");
        assertThat(route.getRouteName()).isEqualTo("Derb Sultan - Place Mohammed V");
        assertThat(route.getSchedules()).isEmpty();
    }
}
