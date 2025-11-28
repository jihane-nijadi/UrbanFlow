package com.uf.routeservice.service;

import com.uf.routeservice.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour RouteService
 * Teste la recherche d'itinéraires et la création d'horaires de bus
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RouteService Unit Tests")
class RouteServiceTest {

    @Mock
    private GeolocationServiceClient geolocationClient;

    @InjectMocks
    private RouteService routeService;

    private BusStop departureStop;
    private BusStop arrivalStop;
    private BusState busState;
    private BusUpdate busUpdate;

    @BeforeEach
    void setUp() {
        // Arrêt de départ
        departureStop = new BusStop();
        departureStop.setStopId("stop-1");
        departureStop.setName("Maarif");
        departureStop.setLat(33.5731);
        departureStop.setLon(-7.5898);
        departureStop.setLineIds(Arrays.asList("L1", "L2", "L3"));

        // Arrêt d'arrivée
        arrivalStop = new BusStop();
        arrivalStop.setStopId("stop-2");
        arrivalStop.setName("Ain Sebaa");
        arrivalStop.setLat(33.5831);
        arrivalStop.setLon(-7.5998);
        arrivalStop.setLineIds(Arrays.asList("L1", "L4"));

        // État d'un bus
        busState = new BusState();
        busState.setBusId("bus-123");
        busState.setLineId("L1");
        busState.setLat(33.5750);
        busState.setLon(-7.5900);
        busState.setSpeed(40.0);
        busState.setHeading(90);
        busState.setLastUpdatedAt(Instant.now());
        busState.setScheduledDepartureTime("09:00");

        // Mise à jour de position
        busUpdate = new BusUpdate();
        busUpdate.setBusId("bus-123");
        busUpdate.setLineId("L1");
        busUpdate.setLat(33.5750);
        busUpdate.setLon(-7.5900);
        busUpdate.setSpeed(40.0);
        busUpdate.setHeading(90);
        busUpdate.setTs(System.currentTimeMillis());
    }

    // ==================== Tests findRoutes ====================

    @Test
    @DisplayName("Should successfully find routes when both stops exist and have common lines")
    void whenFindRoutes_withValidStopsAndCommonLines_thenReturnRoutes() {
        // Given
        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.of(departureStop));
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.of(arrivalStop));
        when(geolocationClient.getAllBuses()).thenReturn(Arrays.asList(busState));
        when(geolocationClient.getBusPositions(anyString(), anyInt())).thenReturn(Arrays.asList(busUpdate));
        when(geolocationClient.getAllStops()).thenReturn(Arrays.asList(departureStop, arrivalStop));

        // When
        RouteResponse result = routeService.findRoutes("stop-1", "stop-2");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDepartureStop()).isEqualTo(departureStop);
        assertThat(result.getArrivalStop()).isEqualTo(arrivalStop);
        assertThat(result.getAvailableBuses()).isNotEmpty();
        assertThat(result.getTotalBuses()).isEqualTo(1);

        verify(geolocationClient, times(1)).getStopById("stop-1");
        verify(geolocationClient, times(1)).getStopById("stop-2");
        verify(geolocationClient, times(1)).getAllBuses();
    }

    @Test
    @DisplayName("Should return empty list when departure stop not found")
    void whenFindRoutes_withInvalidDepartureStop_thenReturnEmptyList() {
        // Given
        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.empty());
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.of(arrivalStop));

        // When
        RouteResponse result = routeService.findRoutes("stop-1", "stop-2");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDepartureStop()).isNull();
        assertThat(result.getArrivalStop()).isEqualTo(arrivalStop);
        assertThat(result.getAvailableBuses()).isEmpty();
        assertThat(result.getTotalBuses()).isEqualTo(0);

        verify(geolocationClient, never()).getAllBuses();
    }

    @Test
    @DisplayName("Should return empty list when arrival stop not found")
    void whenFindRoutes_withInvalidArrivalStop_thenReturnEmptyList() {
        // Given
        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.of(departureStop));
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.empty());

        // When
        RouteResponse result = routeService.findRoutes("stop-1", "stop-2");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDepartureStop()).isEqualTo(departureStop);
        assertThat(result.getArrivalStop()).isNull();
        assertThat(result.getAvailableBuses()).isEmpty();
        assertThat(result.getTotalBuses()).isEqualTo(0);

        verify(geolocationClient, never()).getAllBuses();
    }

    @Test
    @DisplayName("Should return empty list when stops have no common lines")
    void whenFindRoutes_withNoCommonLines_thenReturnEmptyList() {
        // Given
        BusStop stop1 = new BusStop();
        stop1.setStopId("stop-1");
        stop1.setLineIds(Arrays.asList("L5", "L6"));

        BusStop stop2 = new BusStop();
        stop2.setStopId("stop-2");
        stop2.setLineIds(Arrays.asList("L7", "L8"));

        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.of(stop1));
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.of(stop2));

        // When
        RouteResponse result = routeService.findRoutes("stop-1", "stop-2");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAvailableBuses()).isEmpty();
        assertThat(result.getTotalBuses()).isEqualTo(0);

        verify(geolocationClient, never()).getAllBuses();
    }

    @Test
    @DisplayName("Should filter buses by common lines only")
    void whenFindRoutes_thenFilterBusesByCommonLines() {
        // Given
        BusState busL1 = new BusState();
        busL1.setBusId("bus-1");
        busL1.setLineId("L1");

        BusState busL2 = new BusState();
        busL2.setBusId("bus-2");
        busL2.setLineId("L2");

        BusState busL5 = new BusState();
        busL5.setBusId("bus-5");
        busL5.setLineId("L5");

        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.of(departureStop));
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.of(arrivalStop));
        when(geolocationClient.getAllBuses()).thenReturn(Arrays.asList(busL1, busL2, busL5));
        when(geolocationClient.getBusPositions(anyString(), anyInt())).thenReturn(Arrays.asList(busUpdate));
        when(geolocationClient.getAllStops()).thenReturn(Arrays.asList(departureStop, arrivalStop));

        // When
        RouteResponse result = routeService.findRoutes("stop-1", "stop-2");

        // Then
        assertThat(result.getAvailableBuses()).hasSize(1);
        assertThat(result.getAvailableBuses().get(0).getLineId()).isEqualTo("L1");
    }

    @Test
    @DisplayName("Should handle null lineIds in stops")
    void whenFindRoutes_withNullLineIds_thenReturnEmptyList() {
        // Given
        BusStop stop1 = new BusStop();
        stop1.setStopId("stop-1");
        stop1.setLineIds(null);

        BusStop stop2 = new BusStop();
        stop2.setStopId("stop-2");
        stop2.setLineIds(null);

        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.of(stop1));
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.of(stop2));

        // When
        RouteResponse result = routeService.findRoutes("stop-1", "stop-2");

        // Then
        assertThat(result.getAvailableBuses()).isEmpty();
    }

    // ==================== Tests getBusRoute ====================

    @Test
    @DisplayName("Should successfully get bus route when all parameters are valid")
    void whenGetBusRoute_withValidParameters_thenReturnBusSchedule() {
        // Given
        when(geolocationClient.getBusById("bus-123")).thenReturn(Optional.of(busState));
        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.of(departureStop));
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.of(arrivalStop));
        when(geolocationClient.getBusPositions(anyString(), anyInt())).thenReturn(Arrays.asList(busUpdate));
        when(geolocationClient.getAllStops()).thenReturn(Arrays.asList(departureStop, arrivalStop));

        // When
        Optional<BusSchedule> result = routeService.getBusRoute("bus-123", "stop-1", "stop-2");

        // Then
        assertThat(result).isPresent();
        BusSchedule schedule = result.get();
        assertThat(schedule.getBusId()).isEqualTo("bus-123");
        assertThat(schedule.getLineId()).isEqualTo("L1");
        assertThat(schedule.getDepartureStop()).isEqualTo(departureStop);
        assertThat(schedule.getArrivalStop()).isEqualTo(arrivalStop);

        verify(geolocationClient, times(1)).getBusById("bus-123");
        verify(geolocationClient, times(1)).getStopById("stop-1");
        verify(geolocationClient, times(1)).getStopById("stop-2");
    }

    @Test
    @DisplayName("Should return empty when bus not found")
    void whenGetBusRoute_withInvalidBusId_thenReturnEmpty() {
        // Given
        when(geolocationClient.getBusById("bus-999")).thenReturn(Optional.empty());
        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.of(departureStop));
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.of(arrivalStop));

        // When
        Optional<BusSchedule> result = routeService.getBusRoute("bus-999", "stop-1", "stop-2");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when departure stop not found")
    void whenGetBusRoute_withInvalidDepartureStop_thenReturnEmpty() {
        // Given
        when(geolocationClient.getBusById("bus-123")).thenReturn(Optional.of(busState));
        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.empty());
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.of(arrivalStop));

        // When
        Optional<BusSchedule> result = routeService.getBusRoute("bus-123", "stop-1", "stop-2");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when arrival stop not found")
    void whenGetBusRoute_withInvalidArrivalStop_thenReturnEmpty() {
        // Given
        when(geolocationClient.getBusById("bus-123")).thenReturn(Optional.of(busState));
        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.of(departureStop));
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.empty());

        // When
        Optional<BusSchedule> result = routeService.getBusRoute("bus-123", "stop-1", "stop-2");

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== Tests BusSchedule Creation ====================

    @Test
    @DisplayName("Should create bus schedule with scheduled departure time when available")
    void whenCreateBusSchedule_withScheduledDepartureTime_thenUseIt() {
        // Given
        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.of(departureStop));
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.of(arrivalStop));
        when(geolocationClient.getAllBuses()).thenReturn(Arrays.asList(busState));
        when(geolocationClient.getBusPositions(anyString(), anyInt())).thenReturn(Arrays.asList(busUpdate));
        when(geolocationClient.getAllStops()).thenReturn(Arrays.asList(departureStop, arrivalStop));

        // When
        RouteResponse result = routeService.findRoutes("stop-1", "stop-2");

        // Then
        assertThat(result.getAvailableBuses()).isNotEmpty();
        BusSchedule schedule = result.getAvailableBuses().get(0);
        assertThat(schedule.getEstimatedDepartureTime()).isNotNull();
        assertThat(schedule.getEstimatedArrivalTime()).isNotNull();
        assertThat(schedule.getEstimatedDurationMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should create bus schedule with route points from bus positions")
    void whenCreateBusSchedule_withBusPositions_thenCreateRoutePoints() {
        // Given
        BusUpdate pos1 = new BusUpdate();
        pos1.setLat(33.5731);
        pos1.setLon(-7.5898);

        BusUpdate pos2 = new BusUpdate();
        pos2.setLat(33.5800);
        pos2.setLon(-7.5950);

        BusUpdate pos3 = new BusUpdate();
        pos3.setLat(33.5831);
        pos3.setLon(-7.5998);

        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.of(departureStop));
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.of(arrivalStop));
        when(geolocationClient.getAllBuses()).thenReturn(Arrays.asList(busState));
        when(geolocationClient.getBusPositions(anyString(), anyInt()))
                .thenReturn(Arrays.asList(pos1, pos2, pos3));
        when(geolocationClient.getAllStops()).thenReturn(Arrays.asList(departureStop, arrivalStop));

        // When
        RouteResponse result = routeService.findRoutes("stop-1", "stop-2");

        // Then
        assertThat(result.getAvailableBuses()).isNotEmpty();
        BusSchedule schedule = result.getAvailableBuses().get(0);
        assertThat(schedule.getRoutePoints()).isNotEmpty();
        assertThat(schedule.getRoutePoints().size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should create simple route points when no bus positions available")
    void whenCreateBusSchedule_withNoPositions_thenCreateSimpleRoute() {
        // Given
        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.of(departureStop));
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.of(arrivalStop));
        when(geolocationClient.getAllBuses()).thenReturn(Arrays.asList(busState));
        when(geolocationClient.getBusPositions(anyString(), anyInt())).thenReturn(List.of());
        when(geolocationClient.getAllStops()).thenReturn(Arrays.asList(departureStop, arrivalStop));

        // When
        RouteResponse result = routeService.findRoutes("stop-1", "stop-2");

        // Then
        assertThat(result.getAvailableBuses()).isNotEmpty();
        BusSchedule schedule = result.getAvailableBuses().get(0);
        assertThat(schedule.getRoutePoints()).hasSize(2);
        assertThat(schedule.getRoutePoints().get(0).getLat()).isEqualTo(departureStop.getLat());
        assertThat(schedule.getRoutePoints().get(1).getLat()).isEqualTo(arrivalStop.getLat());
    }

    @Test
    @DisplayName("Should set current position and speed in bus schedule")
    void whenCreateBusSchedule_thenSetCurrentPositionAndSpeed() {
        // Given
        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.of(departureStop));
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.of(arrivalStop));
        when(geolocationClient.getAllBuses()).thenReturn(Arrays.asList(busState));
        when(geolocationClient.getBusPositions(anyString(), anyInt())).thenReturn(Arrays.asList(busUpdate));
        when(geolocationClient.getAllStops()).thenReturn(Arrays.asList(departureStop, arrivalStop));

        // When
        RouteResponse result = routeService.findRoutes("stop-1", "stop-2");

        // Then
        assertThat(result.getAvailableBuses()).isNotEmpty();
        BusSchedule schedule = result.getAvailableBuses().get(0);
        assertThat(schedule.getCurrentLat()).isEqualTo(busState.getLat());
        assertThat(schedule.getCurrentLon()).isEqualTo(busState.getLon());
        assertThat(schedule.getCurrentSpeed()).isEqualTo(busState.getSpeed());
        assertThat(schedule.getCurrentHeading()).isEqualTo(busState.getHeading());
    }

    @Test
    @DisplayName("Should calculate estimated times based on distance when no scheduled time")
    void whenCreateBusSchedule_withoutScheduledTime_thenCalculateFromDistance() {
        // Given
        BusState busWithoutSchedule = new BusState();
        busWithoutSchedule.setBusId("bus-456");
        busWithoutSchedule.setLineId("L1");
        busWithoutSchedule.setLat(33.5750);
        busWithoutSchedule.setLon(-7.5900);
        busWithoutSchedule.setSpeed(40.0);
        busWithoutSchedule.setScheduledDepartureTime(null);

        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.of(departureStop));
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.of(arrivalStop));
        when(geolocationClient.getAllBuses()).thenReturn(Arrays.asList(busWithoutSchedule));
        when(geolocationClient.getBusPositions(anyString(), anyInt())).thenReturn(Arrays.asList(busUpdate));
        when(geolocationClient.getAllStops()).thenReturn(Arrays.asList(departureStop, arrivalStop));

        // When
        RouteResponse result = routeService.findRoutes("stop-1", "stop-2");

        // Then
        assertThat(result.getAvailableBuses()).isNotEmpty();
        BusSchedule schedule = result.getAvailableBuses().get(0);
        assertThat(schedule.getEstimatedDepartureTime()).isNotNull();
        assertThat(schedule.getEstimatedArrivalTime()).isNotNull();
        assertThat(schedule.getEstimatedDurationMinutes()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should find intermediate stops between departure and arrival")
    void whenCreateBusSchedule_thenFindIntermediateStops() {
        // Given
        BusStop intermediateStop = new BusStop();
        intermediateStop.setStopId("stop-inter");
        intermediateStop.setName("Intermediate Stop");
        intermediateStop.setLat(33.5780);
        intermediateStop.setLon(-7.5920);
        intermediateStop.setLineIds(Arrays.asList("L1"));

        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.of(departureStop));
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.of(arrivalStop));
        when(geolocationClient.getAllBuses()).thenReturn(Arrays.asList(busState));
        when(geolocationClient.getBusPositions(anyString(), anyInt())).thenReturn(Arrays.asList(busUpdate));
        when(geolocationClient.getAllStops())
                .thenReturn(Arrays.asList(departureStop, intermediateStop, arrivalStop));

        // When
        RouteResponse result = routeService.findRoutes("stop-1", "stop-2");

        // Then
        assertThat(result.getAvailableBuses()).isNotEmpty();
        BusSchedule schedule = result.getAvailableBuses().get(0);
        assertThat(schedule.getIntermediateStops()).isNotNull();
    }

    @Test
    @DisplayName("Should handle empty bus list gracefully")
    void whenFindRoutes_withNoBuses_thenReturnEmptyList() {
        // Given
        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.of(departureStop));
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.of(arrivalStop));
        when(geolocationClient.getAllBuses()).thenReturn(List.of());

        // When
        RouteResponse result = routeService.findRoutes("stop-1", "stop-2");

        // Then
        assertThat(result.getAvailableBuses()).isEmpty();
        assertThat(result.getTotalBuses()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle geolocation service returning empty lists gracefully")
    void whenFindRoutes_withGeolocationServiceErrors_thenHandleGracefully() {
        // Given
        when(geolocationClient.getStopById("stop-1")).thenReturn(Optional.of(departureStop));
        when(geolocationClient.getStopById("stop-2")).thenReturn(Optional.of(arrivalStop));
        when(geolocationClient.getAllBuses()).thenReturn(List.of());

        // When
        RouteResponse result = routeService.findRoutes("stop-1", "stop-2");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAvailableBuses()).isEmpty();
    }
}
