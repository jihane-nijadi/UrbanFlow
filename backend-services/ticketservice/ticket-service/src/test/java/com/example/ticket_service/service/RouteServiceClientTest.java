package com.example.ticket_service.service;

import com.example.ticket_service.dto.RouteDTO;
import com.example.ticket_service.dto.ScheduleDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RouteServiceClient
 * Tests the integration between Ticket Service and Route Service
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RouteServiceClient Unit Tests")
class RouteServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private RouteServiceClient routeServiceClient;

    private static final String ROUTE_SERVICE_URL = "http://localhost:8081";
    private static final Long ROUTE_ID = 1L;
    private static final Long SCHEDULE_ID = 5L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(routeServiceClient, "routeServiceUrl", ROUTE_SERVICE_URL);
    }

    // ==================== getRouteById Tests ====================

    @Test
    @DisplayName("Should successfully fetch route by ID")
    void whenGetRouteById_thenReturnRoute() {
        // Given
        RouteDTO mockRoute = RouteDTO.builder()
                .id(ROUTE_ID)
                .routeId("R10")
                .routeName("Maarif - Ain Sebaa")
                .build();

        when(restTemplate.getForObject(
                eq(ROUTE_SERVICE_URL + "/api/routes/" + ROUTE_ID),
                eq(RouteDTO.class)
        )).thenReturn(mockRoute);

        // When
        RouteDTO result = routeServiceClient.getRouteById(ROUTE_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ROUTE_ID);
        assertThat(result.getRouteId()).isEqualTo("R10");
        assertThat(result.getRouteName()).isEqualTo("Maarif - Ain Sebaa");
        
        verify(restTemplate, times(1)).getForObject(anyString(), eq(RouteDTO.class));
    }

    @Test
    @DisplayName("Should throw exception when route not found")
    void whenGetRouteById_andRouteNotFound_thenThrowException() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(RouteDTO.class)))
                .thenThrow(new RestClientException("404 Not Found"));

        // When & Then
        assertThatThrownBy(() -> routeServiceClient.getRouteById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Route service unavailable or route not found");
        
        verify(restTemplate, times(1)).getForObject(anyString(), eq(RouteDTO.class));
    }

    @Test
    @DisplayName("Should throw exception when route service is unavailable")
    void whenGetRouteById_andServiceUnavailable_thenThrowException() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(RouteDTO.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // When & Then
        assertThatThrownBy(() -> routeServiceClient.getRouteById(ROUTE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Route service unavailable");
        
        verify(restTemplate, times(1)).getForObject(anyString(), eq(RouteDTO.class));
    }

    // ==================== getScheduleById Tests ====================

    @Test
    @DisplayName("Should successfully fetch schedule by ID")
    void whenGetScheduleById_thenReturnSchedule() {
        // Given
        ScheduleDTO mockSchedule = ScheduleDTO.builder()
                .id(SCHEDULE_ID)
                .routeId("R10")
                .departureStop("Maarif")
                .arrivalStop("Casa Port")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .build();

        when(restTemplate.getForObject(
                eq(ROUTE_SERVICE_URL + "/api/routes/schedules/" + SCHEDULE_ID),
                eq(ScheduleDTO.class)
        )).thenReturn(mockSchedule);

        // When
        ScheduleDTO result = routeServiceClient.getScheduleById(SCHEDULE_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(SCHEDULE_ID);
        assertThat(result.getRouteId()).isEqualTo("R10");
        assertThat(result.getDepartureStop()).isEqualTo("Maarif");
        assertThat(result.getArrivalStop()).isEqualTo("Casa Port");
        assertThat(result.getDepartureTime()).isEqualTo("08:00");
        assertThat(result.getArrivalTime()).isEqualTo("08:25");
        
        verify(restTemplate, times(1)).getForObject(anyString(), eq(ScheduleDTO.class));
    }

    @Test
    @DisplayName("Should throw exception when schedule not found")
    void whenGetScheduleById_andScheduleNotFound_thenThrowException() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(ScheduleDTO.class)))
                .thenThrow(new RestClientException("404 Not Found"));

        // When & Then
        assertThatThrownBy(() -> routeServiceClient.getScheduleById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Route service unavailable or schedule not found");
        
        verify(restTemplate, times(1)).getForObject(anyString(), eq(ScheduleDTO.class));
    }

    @Test
    @DisplayName("Should throw exception when fetching schedule and service unavailable")
    void whenGetScheduleById_andServiceUnavailable_thenThrowException() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(ScheduleDTO.class)))
                .thenThrow(new RestClientException("Connection timeout"));

        // When & Then
        assertThatThrownBy(() -> routeServiceClient.getScheduleById(SCHEDULE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Route service unavailable");
        
        verify(restTemplate, times(1)).getForObject(anyString(), eq(ScheduleDTO.class));
    }

    // ==================== isRouteAvailable Tests ====================

    @Test
    @DisplayName("Should return true when route is available")
    void whenIsRouteAvailable_andRouteExists_thenReturnTrue() {
        // Given
        RouteDTO mockRoute = RouteDTO.builder()
                .id(ROUTE_ID)
                .routeId("R10")
                .routeName("Maarif - Ain Sebaa")
                .build();

        when(restTemplate.getForObject(anyString(), eq(RouteDTO.class)))
                .thenReturn(mockRoute);

        // When
        boolean result = routeServiceClient.isRouteAvailable(ROUTE_ID);

        // Then
        assertThat(result).isTrue();
        verify(restTemplate, times(1)).getForObject(anyString(), eq(RouteDTO.class));
    }

    @Test
    @DisplayName("Should return false when route is not available")
    void whenIsRouteAvailable_andRouteNotFound_thenReturnFalse() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(RouteDTO.class)))
                .thenThrow(new RestClientException("404 Not Found"));

        // When
        boolean result = routeServiceClient.isRouteAvailable(999L);

        // Then
        assertThat(result).isFalse();
        verify(restTemplate, times(1)).getForObject(anyString(), eq(RouteDTO.class));
    }

    @Test
    @DisplayName("Should return false when route service is down")
    void whenIsRouteAvailable_andServiceDown_thenReturnFalse() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(RouteDTO.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // When
        boolean result = routeServiceClient.isRouteAvailable(ROUTE_ID);

        // Then
        assertThat(result).isFalse();
        verify(restTemplate, times(1)).getForObject(anyString(), eq(RouteDTO.class));
    }

    // ==================== isScheduleAvailable Tests ====================

    @Test
    @DisplayName("Should return true when schedule is available")
    void whenIsScheduleAvailable_andScheduleExists_thenReturnTrue() {
        // Given
        ScheduleDTO mockSchedule = ScheduleDTO.builder()
                .id(SCHEDULE_ID)
                .routeId("R10")
                .departureStop("Maarif")
                .arrivalStop("Casa Port")
                .build();

        when(restTemplate.getForObject(anyString(), eq(ScheduleDTO.class)))
                .thenReturn(mockSchedule);

        // When
        boolean result = routeServiceClient.isScheduleAvailable(SCHEDULE_ID);

        // Then
        assertThat(result).isTrue();
        verify(restTemplate, times(1)).getForObject(anyString(), eq(ScheduleDTO.class));
    }

    @Test
    @DisplayName("Should return false when schedule is not available")
    void whenIsScheduleAvailable_andScheduleNotFound_thenReturnFalse() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(ScheduleDTO.class)))
                .thenThrow(new RestClientException("404 Not Found"));

        // When
        boolean result = routeServiceClient.isScheduleAvailable(999L);

        // Then
        assertThat(result).isFalse();
        verify(restTemplate, times(1)).getForObject(anyString(), eq(ScheduleDTO.class));
    }

    @Test
    @DisplayName("Should return false when checking schedule availability and service is down")
    void whenIsScheduleAvailable_andServiceDown_thenReturnFalse() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(ScheduleDTO.class)))
                .thenThrow(new RestClientException("Service unavailable"));

        // When
        boolean result = routeServiceClient.isScheduleAvailable(SCHEDULE_ID);

        // Then
        assertThat(result).isFalse();
        verify(restTemplate, times(1)).getForObject(anyString(), eq(ScheduleDTO.class));
    }

    // ==================== URL Construction Tests ====================

    @Test
    @DisplayName("Should construct correct URL for route endpoint")
    void whenGetRouteById_thenUseCorrectUrl() {
        // Given
        RouteDTO mockRoute = RouteDTO.builder().id(ROUTE_ID).build();
        String expectedUrl = ROUTE_SERVICE_URL + "/api/routes/" + ROUTE_ID;

        when(restTemplate.getForObject(eq(expectedUrl), eq(RouteDTO.class)))
                .thenReturn(mockRoute);

        // When
        routeServiceClient.getRouteById(ROUTE_ID);

        // Then
        verify(restTemplate).getForObject(eq(expectedUrl), eq(RouteDTO.class));
    }

    @Test
    @DisplayName("Should construct correct URL for schedule endpoint")
    void whenGetScheduleById_thenUseCorrectUrl() {
        // Given
        ScheduleDTO mockSchedule = ScheduleDTO.builder().id(SCHEDULE_ID).build();
        String expectedUrl = ROUTE_SERVICE_URL + "/api/routes/schedules/" + SCHEDULE_ID;

        when(restTemplate.getForObject(eq(expectedUrl), eq(ScheduleDTO.class)))
                .thenReturn(mockSchedule);

        // When
        routeServiceClient.getScheduleById(SCHEDULE_ID);

        // Then
        verify(restTemplate).getForObject(eq(expectedUrl), eq(ScheduleDTO.class));
    }
}
