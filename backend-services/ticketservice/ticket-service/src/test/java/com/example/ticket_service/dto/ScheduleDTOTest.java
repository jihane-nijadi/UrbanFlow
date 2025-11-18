package com.example.ticket_service.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ScheduleDTO to ensure compatibility with Route Service
 */
@DisplayName("ScheduleDTO Compatibility Tests")
class ScheduleDTOTest {

    @Test
    @DisplayName("ScheduleDTO should have all required fields for Route Service compatibility")
    void scheduleDTO_shouldHaveRequiredFields() {
        // When
        ScheduleDTO schedule = ScheduleDTO.builder()
                .id(5L)
                .routeId("R10")
                .departureStop("Maarif")
                .arrivalStop("Casa Port")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .build();

        // Then
        assertThat(schedule).isNotNull();
        assertThat(schedule.getId()).isEqualTo(5L);
        assertThat(schedule.getRouteId()).isEqualTo("R10");
        assertThat(schedule.getDepartureStop()).isEqualTo("Maarif");
        assertThat(schedule.getArrivalStop()).isEqualTo("Casa Port");
        assertThat(schedule.getDepartureTime()).isEqualTo("08:00");
        assertThat(schedule.getArrivalTime()).isEqualTo("08:25");
    }

    @Test
    @DisplayName("ScheduleDTO should be serializable for REST communication")
    void scheduleDTO_shouldBeSerializable() {
        // Given
        ScheduleDTO schedule = ScheduleDTO.builder()
                .id(1L)
                .routeId("R10")
                .departureStop("Maarif")
                .arrivalStop("Zerktouni")
                .departureTime("08:00")
                .arrivalTime("08:10")
                .build();

        // Then - Verify all getters work (required for Jackson serialization)
        assertThat(schedule.getId()).isNotNull();
        assertThat(schedule.getRouteId()).isNotNull();
        assertThat(schedule.getDepartureStop()).isNotNull();
        assertThat(schedule.getArrivalStop()).isNotNull();
        assertThat(schedule.getDepartureTime()).isNotNull();
        assertThat(schedule.getArrivalTime()).isNotNull();
    }

    @Test
    @DisplayName("ScheduleDTO builder should work correctly")
    void scheduleDTO_builderShouldWork() {
        // When
        ScheduleDTO schedule = ScheduleDTO.builder()
                .id(10L)
                .routeId("R20")
                .departureStop("Derb Sultan")
                .arrivalStop("Place Mohammed V")
                .departureTime("09:00")
                .arrivalTime("09:27")
                .build();

        // Then
        assertThat(schedule.getId()).isEqualTo(10L);
        assertThat(schedule.getRouteId()).isEqualTo("R20");
        assertThat(schedule.getDepartureStop()).isEqualTo("Derb Sultan");
        assertThat(schedule.getArrivalStop()).isEqualTo("Place Mohammed V");
        assertThat(schedule.getDepartureTime()).isEqualTo("09:00");
        assertThat(schedule.getArrivalTime()).isEqualTo("09:27");
    }

    @Test
    @DisplayName("ScheduleDTO should handle time format HH:mm")
    void scheduleDTO_shouldHandleTimeFormat() {
        // When
        ScheduleDTO schedule = ScheduleDTO.builder()
                .id(1L)
                .routeId("R10")
                .departureStop("Maarif")
                .arrivalStop("Casa Port")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .build();

        // Then - Verify time format
        assertThat(schedule.getDepartureTime()).matches("\\d{2}:\\d{2}");
        assertThat(schedule.getArrivalTime()).matches("\\d{2}:\\d{2}");
    }

    @Test
    @DisplayName("ScheduleDTO routeId should match Route routeId format")
    void scheduleDTO_routeIdShouldMatchRouteFormat() {
        // Given
        String expectedRouteId = "R10";

        // When
        ScheduleDTO schedule = ScheduleDTO.builder()
                .id(1L)
                .routeId(expectedRouteId)
                .departureStop("Maarif")
                .arrivalStop("Casa Port")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .build();

        // Then
        assertThat(schedule.getRouteId()).isEqualTo(expectedRouteId);
        assertThat(schedule.getRouteId()).matches("R\\d+");
    }
}
