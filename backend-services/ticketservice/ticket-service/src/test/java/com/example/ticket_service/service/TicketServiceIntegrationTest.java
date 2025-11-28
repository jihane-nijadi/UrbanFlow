package com.example.ticket_service.service;

import com.example.ticket_service.dto.RouteDTO;
import com.example.ticket_service.dto.ScheduleDTO;
import com.example.ticket_service.dto.TicketRequest;
import com.example.ticket_service.dto.TicketResponse;
import com.example.ticket_service.entities.Ticket;
import com.example.ticket_service.entities.TicketStatus;
import com.example.ticket_service.exception.ResourceNotFoundException;
import com.example.ticket_service.exception.SeatAlreadyBookedException;
import com.example.ticket_service.mapper.TicketMapper;
import com.example.ticket_service.messaging.publisher.TicketEventPublisher;
import com.example.ticket_service.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Integration tests for TicketService with Route Service
 * Tests the complete ticket creation flow with 70-seat limit validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService Integration Tests - Seat Limit Logic")
class TicketServiceIntegrationTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketEventPublisher eventPublisher;

    @Mock
    private RouteServiceClient routeServiceClient;

    @InjectMocks
    private TicketService ticketService;

    private TicketRequest validRequest;
    private RouteDTO mockRoute;
    private ScheduleDTO mockSchedule;
    private Ticket mockTicket;
    private TicketResponse mockResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        validRequest = TicketRequest.builder()
                .userId("user123")
                .routeId(1L)
                .scheduleId(5L)
                .travelDate(LocalDateTime.now().plusDays(1))
                .price(BigDecimal.valueOf(25.00))
                .build();

        mockRoute = RouteDTO.builder()
                .id(1L)
                .routeId("R10")
                .routeName("Maarif - Ain Sebaa")
                .build();

        mockSchedule = ScheduleDTO.builder()
                .id(5L)
                .routeId("R10")
                .departureStop("Maarif")
                .arrivalStop("Casa Port")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .build();

        mockTicket = Ticket.builder()
                .id(1L)
                .userId("user123")
                .routeId(1L)
                .scheduleId(5L)
                .routeName("Maarif - Ain Sebaa")
                .departureStop("Maarif")
                .arrivalStop("Casa Port")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .travelDate(validRequest.getTravelDate())
                .price(BigDecimal.valueOf(25.00))
                .status(TicketStatus.PENDING)
                .qrCode("qr-code-123")
                .purchaseDate(LocalDateTime.now())
                .build();

        mockResponse = TicketResponse.builder()
                .id(1L)
                .userId("user123")
                .routeId(1L)
                .scheduleId(5L)
                .routeName("Maarif - Ain Sebaa")
                .departureStop("Maarif")
                .arrivalStop("Casa Port")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .travelDate(validRequest.getTravelDate())
                .price(BigDecimal.valueOf(25.00))
                .status(TicketStatus.PENDING)
                .purchaseDate(LocalDateTime.now())
                .build();
    }

    // ==================== Successful Ticket Creation Tests ====================

    @Test
    @DisplayName("Should successfully create ticket when seats are available (0/70)")
    void whenCreateTicket_andNoSeatsBooked_thenSucceed() {
        // Given
        when(routeServiceClient.getRouteById(1L)).thenReturn(mockRoute);
        when(routeServiceClient.getScheduleById(5L)).thenReturn(mockSchedule);
        when(ticketRepository.countByScheduleIdAndTravelDate(5L, validRequest.getTravelDate()))
                .thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(mockResponse);

        // When
        TicketResponse result = ticketService.createTicket(validRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRouteName()).isEqualTo("Maarif - Ain Sebaa");
        assertThat(result.getStatus()).isEqualTo(TicketStatus.PENDING);

        verify(routeServiceClient, times(1)).getRouteById(1L);
        verify(routeServiceClient, times(1)).getScheduleById(5L);
        verify(ticketRepository, times(1)).countByScheduleIdAndTravelDate(5L, validRequest.getTravelDate());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(eventPublisher, times(1)).publishTicketCreated(any());
    }

    @Test
    @DisplayName("Should successfully create ticket when 69 seats are booked (69/70)")
    void whenCreateTicket_and69SeatsBooked_thenSucceed() {
        // Given
        when(routeServiceClient.getRouteById(1L)).thenReturn(mockRoute);
        when(routeServiceClient.getScheduleById(5L)).thenReturn(mockSchedule);
        when(ticketRepository.countByScheduleIdAndTravelDate(5L, validRequest.getTravelDate()))
                .thenReturn(69L); // Last available seat
        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(mockResponse);

        // When
        TicketResponse result = ticketService.createTicket(validRequest);

        // Then
        assertThat(result).isNotNull();
        verify(ticketRepository, times(1)).countByScheduleIdAndTravelDate(5L, validRequest.getTravelDate());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Should create ticket with correct route and schedule information")
    void whenCreateTicket_thenTicketContainsRouteAndScheduleInfo() {
        // Given
        when(routeServiceClient.getRouteById(1L)).thenReturn(mockRoute);
        when(routeServiceClient.getScheduleById(5L)).thenReturn(mockSchedule);
        when(ticketRepository.countByScheduleIdAndTravelDate(anyLong(), any())).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(mockResponse);

        // When
        TicketResponse result = ticketService.createTicket(validRequest);

        // Then
        assertThat(result.getRouteName()).isEqualTo("Maarif - Ain Sebaa");
        assertThat(result.getDepartureStop()).isEqualTo("Maarif");
        assertThat(result.getArrivalStop()).isEqualTo("Casa Port");
        assertThat(result.getDepartureTime()).isEqualTo("08:00");
        assertThat(result.getArrivalTime()).isEqualTo("08:25");
    }

    // ==================== Seat Limit Validation Tests ====================

    @Test
    @DisplayName("Should throw exception when all 70 seats are booked")
    void whenCreateTicket_and70SeatsBooked_thenThrowException() {
        // Given
        when(routeServiceClient.getRouteById(1L)).thenReturn(mockRoute);
        when(routeServiceClient.getScheduleById(5L)).thenReturn(mockSchedule);
        when(ticketRepository.countByScheduleIdAndTravelDate(5L, validRequest.getTravelDate()))
                .thenReturn(70L); // All seats taken

        // When & Then
        assertThatThrownBy(() -> ticketService.createTicket(validRequest))
                .isInstanceOf(SeatAlreadyBookedException.class)
                .hasMessageContaining("All seats are booked")
                .hasMessageContaining("70/70 seats taken");

        verify(ticketRepository, times(1)).countByScheduleIdAndTravelDate(5L, validRequest.getTravelDate());
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(eventPublisher, never()).publishTicketCreated(any());
    }

    @Test
    @DisplayName("Should throw exception when more than 70 seats somehow booked")
    void whenCreateTicket_andOver70SeatsBooked_thenThrowException() {
        // Given
        when(routeServiceClient.getRouteById(1L)).thenReturn(mockRoute);
        when(routeServiceClient.getScheduleById(5L)).thenReturn(mockSchedule);
        when(ticketRepository.countByScheduleIdAndTravelDate(5L, validRequest.getTravelDate()))
                .thenReturn(75L); // Edge case: data corruption scenario

        // When & Then
        assertThatThrownBy(() -> ticketService.createTicket(validRequest))
                .isInstanceOf(SeatAlreadyBookedException.class)
                .hasMessageContaining("All seats are booked");

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Should check seat count for correct schedule and travel date")
    void whenCreateTicket_thenCheckSeatCountWithCorrectParameters() {
        // Given
        LocalDateTime specificDate = LocalDateTime.of(2025, 12, 25, 10, 0);
        validRequest.setTravelDate(specificDate);

        when(routeServiceClient.getRouteById(1L)).thenReturn(mockRoute);
        when(routeServiceClient.getScheduleById(5L)).thenReturn(mockSchedule);
        when(ticketRepository.countByScheduleIdAndTravelDate(5L, specificDate)).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(mockResponse);

        // When
        ticketService.createTicket(validRequest);

        // Then
        verify(ticketRepository, times(1)).countByScheduleIdAndTravelDate(5L, specificDate);
    }

    // ==================== Route/Schedule Validation Tests ====================

    @Test
    @DisplayName("Should create ticket with fallback values when route service is unavailable")
    void whenCreateTicket_andRouteNotFound_thenCreateTicketWithFallback() {
        // Given - Route service throws exception, but service should continue
        when(routeServiceClient.getRouteById(1L))
                .thenThrow(new RuntimeException("Route service unavailable or route not found"));
        when(ticketRepository.countByScheduleIdAndTravelDate(anyLong(), any())).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(mockResponse);

        // When
        TicketResponse result = ticketService.createTicket(validRequest);

        // Then - Ticket should be created with fallback values
        assertThat(result).isNotNull();
        verify(routeServiceClient, times(1)).getRouteById(1L);
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Should create ticket with fallback values when schedule service is unavailable")
    void whenCreateTicket_andScheduleNotFound_thenCreateTicketWithFallback() {
        // Given - Schedule service throws exception, but service should continue
        when(routeServiceClient.getRouteById(1L)).thenReturn(mockRoute);
        when(routeServiceClient.getScheduleById(5L))
                .thenThrow(new RuntimeException("Route service unavailable or schedule not found"));
        when(ticketRepository.countByScheduleIdAndTravelDate(anyLong(), any())).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(mockResponse);

        // When
        TicketResponse result = ticketService.createTicket(validRequest);

        // Then - Ticket should be created with fallback values
        assertThat(result).isNotNull();
        verify(routeServiceClient, times(1)).getRouteById(1L);
        verify(routeServiceClient, times(1)).getScheduleById(5L);
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Should throw exception when schedule does not belong to route")
    void whenCreateTicket_andScheduleNotBelongToRoute_thenThrowException() {
        // Given
        ScheduleDTO wrongSchedule = ScheduleDTO.builder()
                .id(5L)
                .routeId("R20") // Different route
                .departureStop("Different Stop")
                .arrivalStop("Another Stop")
                .build();

        when(routeServiceClient.getRouteById(1L)).thenReturn(mockRoute);
        when(routeServiceClient.getScheduleById(5L)).thenReturn(wrongSchedule);

        // When & Then
        assertThatThrownBy(() -> ticketService.createTicket(validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Schedule 5 does not belong to route 1");

        verify(ticketRepository, never()).countByScheduleIdAndTravelDate(anyLong(), any());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // ==================== Event Publishing Tests ====================

    @Test
    @DisplayName("Should publish ticket created event after successful creation")
    void whenCreateTicket_thenPublishTicketCreatedEvent() {
        // Given
        when(routeServiceClient.getRouteById(1L)).thenReturn(mockRoute);
        when(routeServiceClient.getScheduleById(5L)).thenReturn(mockSchedule);
        when(ticketRepository.countByScheduleIdAndTravelDate(anyLong(), any())).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(mockResponse);

        // When
        ticketService.createTicket(validRequest);

        // Then
        verify(eventPublisher, times(1)).publishTicketCreated(any());
    }

    @Test
    @DisplayName("Should not publish event when seat limit reached")
    void whenCreateTicket_andSeatLimitReached_thenDoNotPublishEvent() {
        // Given
        when(routeServiceClient.getRouteById(1L)).thenReturn(mockRoute);
        when(routeServiceClient.getScheduleById(5L)).thenReturn(mockSchedule);
        when(ticketRepository.countByScheduleIdAndTravelDate(5L, validRequest.getTravelDate()))
                .thenReturn(70L);

        // When & Then
        assertThatThrownBy(() -> ticketService.createTicket(validRequest))
                .isInstanceOf(SeatAlreadyBookedException.class);

        verify(eventPublisher, never()).publishTicketCreated(any());
    }

    // ==================== Ticket Retrieval Tests ====================

    @Test
    @DisplayName("Should successfully retrieve ticket by ID")
    void whenGetTicketById_thenReturnTicket() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(mockTicket));
        when(ticketMapper.toResponse(mockTicket)).thenReturn(mockResponse);

        // When
        TicketResponse result = ticketService.getTicketById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(ticketRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when ticket not found")
    void whenGetTicketById_andNotFound_thenThrowException() {
        // Given
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> ticketService.getTicketById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ticket not found with ID: 999");

        verify(ticketRepository, times(1)).findById(999L);
    }

    // ==================== Ticket Cancellation Tests ====================

    @Test
    @DisplayName("Should successfully cancel active ticket")
    void whenCancelTicket_andTicketActive_thenSucceed() {
        // Given
        mockTicket.setStatus(TicketStatus.ACTIVE);
        when(ticketRepository.findByIdAndUserId(1L, "user123")).thenReturn(Optional.of(mockTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(mockResponse);

        // When
        TicketResponse result = ticketService.cancelTicket(1L, "user123");

        // Then
        assertThat(result).isNotNull();
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(eventPublisher, times(1)).publishTicketCancelled(any());
    }

    @Test
    @DisplayName("Should throw exception when cancelling non-active ticket")
    void whenCancelTicket_andTicketNotActive_thenThrowException() {
        // Given
        mockTicket.setStatus(TicketStatus.USED);
        when(ticketRepository.findByIdAndUserId(1L, "user123")).thenReturn(Optional.of(mockTicket));

        // When & Then
        assertThatThrownBy(() -> ticketService.cancelTicket(1L, "user123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only active tickets can be cancelled");

        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(eventPublisher, never()).publishTicketCancelled(any());
    }

    // ==================== Concurrent Booking Scenario Tests ====================

    @Test
    @DisplayName("Should handle concurrent booking attempt at seat limit boundary")
    void whenMultipleRequests_atSeatLimitBoundary_thenOnlyAllowUpTo70() {
        // Simulate race condition: two requests checking simultaneously when 69 seats booked
        // This test verifies the logic checks the count each time
        
        // Given - First request sees 69 seats
        when(routeServiceClient.getRouteById(1L)).thenReturn(mockRoute);
        when(routeServiceClient.getScheduleById(5L)).thenReturn(mockSchedule);
        when(ticketRepository.countByScheduleIdAndTravelDate(5L, validRequest.getTravelDate()))
                .thenReturn(69L); // First call: 69 seats
        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(mockResponse);

        // When - First request should succeed
        TicketResponse result = ticketService.createTicket(validRequest);

        // Then
        assertThat(result).isNotNull();
        verify(ticketRepository, times(1)).save(any(Ticket.class));

        // Given - Second request sees 70 seats (after first was saved)
        when(ticketRepository.countByScheduleIdAndTravelDate(5L, validRequest.getTravelDate()))
                .thenReturn(70L);

        // When & Then - Second request should fail
        assertThatThrownBy(() -> ticketService.createTicket(validRequest))
                .isInstanceOf(SeatAlreadyBookedException.class)
                .hasMessageContaining("70/70 seats taken");
    }

    @Test
    @DisplayName("Should save ticket with PENDING status initially")
    void whenCreateTicket_thenTicketStatusIsPending() {
        // Given
        when(routeServiceClient.getRouteById(1L)).thenReturn(mockRoute);
        when(routeServiceClient.getScheduleById(5L)).thenReturn(mockSchedule);
        when(ticketRepository.countByScheduleIdAndTravelDate(anyLong(), any())).thenReturn(0L);
        
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        when(ticketRepository.save(ticketCaptor.capture())).thenReturn(mockTicket);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(mockResponse);

        // When
        ticketService.createTicket(validRequest);

        // Then
        Ticket savedTicket = ticketCaptor.getValue();
        assertThat(savedTicket.getStatus()).isEqualTo(TicketStatus.PENDING);
    }

    @Test
    @DisplayName("Should generate QR code for new ticket")
    void whenCreateTicket_thenGenerateQRCode() {
        // Given
        when(routeServiceClient.getRouteById(1L)).thenReturn(mockRoute);
        when(routeServiceClient.getScheduleById(5L)).thenReturn(mockSchedule);
        when(ticketRepository.countByScheduleIdAndTravelDate(anyLong(), any())).thenReturn(0L);
        
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        when(ticketRepository.save(ticketCaptor.capture())).thenReturn(mockTicket);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(mockResponse);

        // When
        ticketService.createTicket(validRequest);

        // Then
        Ticket savedTicket = ticketCaptor.getValue();
        assertThat(savedTicket.getQrCode()).isNotNull();
    }
}
