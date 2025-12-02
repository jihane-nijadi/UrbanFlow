package com.example.ticket_service.integration;

import com.example.ticket_service.dto.RouteDTO;
import com.example.ticket_service.dto.ScheduleDTO;
import com.example.ticket_service.dto.TicketRequest;
import com.example.ticket_service.dto.TicketResponse;
import com.example.ticket_service.entities.Ticket;
import com.example.ticket_service.entities.TicketStatus;
import com.example.ticket_service.exception.ResourceNotFoundException;
import com.example.ticket_service.exception.SeatAlreadyBookedException;
import com.example.ticket_service.repository.TicketRepository;
import com.example.ticket_service.service.RouteServiceClient;
import com.example.ticket_service.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests d'intégration pour TicketService avec base de données réelle
 * Teste l'intégration complète entre le service et le repository
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Tests d'intégration - TicketService avec Base de Données")
class TicketServiceIntegrationTestWithDB {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @MockBean
    private RouteServiceClient routeServiceClient;

    private TicketRequest validRequest;
    private RouteDTO mockRoute;
    private ScheduleDTO mockSchedule;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();

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

        when(routeServiceClient.getRouteById(1L)).thenReturn(mockRoute);
        when(routeServiceClient.getScheduleById(5L)).thenReturn(mockSchedule);
    }

    // ==================== Tests de création avec base de données ====================

    @Test
    @DisplayName("Devrait créer et sauvegarder un ticket en base de données")
    void whenCreateTicket_thenTicketIsPersisted() {
        // When
        TicketResponse response = ticketService.createTicket(validRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();

        // Vérifier en base
        Ticket savedTicket = ticketRepository.findById(response.getId()).orElseThrow();
        assertThat(savedTicket.getUserId()).isEqualTo("user123");
        assertThat(savedTicket.getRouteId()).isEqualTo(1L);
        assertThat(savedTicket.getScheduleId()).isEqualTo(5L);
        assertThat(savedTicket.getStatus()).isEqualTo(TicketStatus.PENDING);
        assertThat(savedTicket.getQrCode()).isNotNull();
        assertThat(savedTicket.getPurchaseDate()).isNotNull();
    }

    @Test
    @DisplayName("Devrait respecter la limite de 70 sièges par schedule et date")
    void whenCreateTicket_and70SeatsBooked_thenThrowException() {
        // Given - Créer 70 tickets
        LocalDateTime travelDate = validRequest.getTravelDate();
        for (int i = 0; i < 70; i++) {
            Ticket ticket = Ticket.builder()
                    .userId("user" + i)
                    .routeId(1L)
                    .scheduleId(5L)
                    .routeName("Test Route")
                    .departureStop("A")
                    .arrivalStop("B")
                    .departureTime("08:00")
                    .arrivalTime("08:25")
                    .travelDate(travelDate)
                    .price(BigDecimal.valueOf(25.00))
                    .status(TicketStatus.PENDING)
                    .build();
            ticketRepository.save(ticket);
        }

        // When & Then
        assertThatThrownBy(() -> ticketService.createTicket(validRequest))
                .isInstanceOf(SeatAlreadyBookedException.class)
                .hasMessageContaining("All seats are booked");

        // Vérifier qu'aucun nouveau ticket n'a été créé
        assertThat(ticketRepository.count()).isEqualTo(70);
    }

    @Test
    @DisplayName("Devrait permettre la création jusqu'à 69 sièges")
    void whenCreateTicket_and69SeatsBooked_thenSucceed() {
        // Given - Créer 69 tickets
        LocalDateTime travelDate = validRequest.getTravelDate();
        for (int i = 0; i < 69; i++) {
            Ticket ticket = Ticket.builder()
                    .userId("user" + i)
                    .routeId(1L)
                    .scheduleId(5L)
                    .routeName("Test Route")
                    .departureStop("A")
                    .arrivalStop("B")
                    .departureTime("08:00")
                    .arrivalTime("08:25")
                    .travelDate(travelDate)
                    .price(BigDecimal.valueOf(25.00))
                    .status(TicketStatus.PENDING)
                    .build();
            ticketRepository.save(ticket);
        }

        // When
        TicketResponse response = ticketService.createTicket(validRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(ticketRepository.count()).isEqualTo(70);
    }

    @Test
    @DisplayName("Devrait compter les sièges uniquement pour le même schedule et date")
    void whenCreateTicket_thenCountOnlySameScheduleAndDate() {
        // Given - Créer 70 tickets pour schedule 5, date X
        LocalDateTime date1 = LocalDateTime.now().plusDays(1);
        for (int i = 0; i < 70; i++) {
            Ticket ticket = Ticket.builder()
                    .userId("user" + i)
                    .routeId(1L)
                    .scheduleId(5L)
                    .routeName("Test Route")
                    .departureStop("A")
                    .arrivalStop("B")
                    .departureTime("08:00")
                    .arrivalTime("08:25")
                    .travelDate(date1)
                    .price(BigDecimal.valueOf(25.00))
                    .status(TicketStatus.PENDING)
                    .build();
            ticketRepository.save(ticket);
        }

        // When - Essayer de créer un ticket pour schedule 5, date Y (différente)
        TicketRequest requestDifferentDate = TicketRequest.builder()
                .userId("newuser")
                .routeId(1L)
                .scheduleId(5L)
                .travelDate(LocalDateTime.now().plusDays(2)) // Date différente
                .price(BigDecimal.valueOf(25.00))
                .build();

        TicketResponse response = ticketService.createTicket(requestDifferentDate);

        // Then - Devrait réussir car c'est une date différente
        assertThat(response).isNotNull();
        assertThat(ticketRepository.count()).isEqualTo(71);
    }

    // ==================== Tests de récupération ====================

    @Test
    @DisplayName("Devrait récupérer un ticket par ID depuis la base")
    void whenGetTicketById_thenReturnFromDatabase() {
        // Given
        Ticket ticket = Ticket.builder()
                .userId("user123")
                .routeId(1L)
                .scheduleId(5L)
                .routeName("Test Route")
                .departureStop("A")
                .arrivalStop("B")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .travelDate(LocalDateTime.now().plusDays(1))
                .price(BigDecimal.valueOf(25.00))
                .status(TicketStatus.ACTIVE)
                .build();
        Ticket savedTicket = ticketRepository.save(ticket);

        // When
        TicketResponse response = ticketService.getTicketById(savedTicket.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(savedTicket.getId());
        assertThat(response.getUserId()).isEqualTo("user123");
    }

    @Test
    @DisplayName("Devrait lancer une exception si ticket non trouvé")
    void whenGetTicketById_andNotFound_thenThrowException() {
        // When & Then
        assertThatThrownBy(() -> ticketService.getTicketById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ticket not found with ID: 999");
    }

    @Test
    @DisplayName("Devrait récupérer tous les tickets d'un utilisateur")
    void whenGetTicketsByUserId_thenReturnAllUserTickets() {
        // Given
        String userId = "user123";
        for (int i = 0; i < 3; i++) {
            Ticket ticket = Ticket.builder()
                    .userId(userId)
                    .routeId(1L)
                    .scheduleId(5L)
                    .routeName("Route " + i)
                    .departureStop("A")
                    .arrivalStop("B")
                    .departureTime("08:00")
                    .arrivalTime("08:25")
                    .travelDate(LocalDateTime.now().plusDays(i + 1))
                    .price(BigDecimal.valueOf(25.00))
                    .status(TicketStatus.ACTIVE)
                    .build();
            ticketRepository.save(ticket);
        }

        // When
        List<TicketResponse> tickets = ticketService.getTicketsByUserId(userId);

        // Then
        assertThat(tickets).hasSize(3);
        assertThat(tickets).allMatch(t -> t.getUserId().equals(userId));
    }

    @Test
    @DisplayName("Devrait récupérer uniquement les tickets actifs d'un utilisateur")
    void whenGetActiveTickets_thenReturnOnlyActiveTickets() {
        // Given
        String userId = "user123";

        Ticket activeTicket1 = Ticket.builder()
                .userId(userId)
                .routeId(1L)
                .scheduleId(5L)
                .routeName("Active 1")
                .departureStop("A")
                .arrivalStop("B")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .travelDate(LocalDateTime.now().plusDays(1))
                .price(BigDecimal.valueOf(25.00))
                .status(TicketStatus.ACTIVE)
                .build();
        ticketRepository.save(activeTicket1);

        Ticket activeTicket2 = Ticket.builder()
                .userId(userId)
                .routeId(1L)
                .scheduleId(5L)
                .routeName("Active 2")
                .departureStop("A")
                .arrivalStop("B")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .travelDate(LocalDateTime.now().plusDays(2))
                .price(BigDecimal.valueOf(25.00))
                .status(TicketStatus.ACTIVE)
                .build();
        ticketRepository.save(activeTicket2);

        Ticket cancelledTicket = Ticket.builder()
                .userId(userId)
                .routeId(1L)
                .scheduleId(5L)
                .routeName("Cancelled")
                .departureStop("A")
                .arrivalStop("B")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .travelDate(LocalDateTime.now().plusDays(3))
                .price(BigDecimal.valueOf(25.00))
                .status(TicketStatus.CANCELLED)
                .build();
        ticketRepository.save(cancelledTicket);

        // When
        List<TicketResponse> activeTickets = ticketService.getActiveTickets(userId);

        // Then
        assertThat(activeTickets).hasSize(2);
        assertThat(activeTickets).allMatch(t -> t.getStatus() == TicketStatus.ACTIVE);
    }

    @Test
    @DisplayName("Devrait récupérer uniquement les tickets à venir")
    void whenGetUpcomingTickets_thenReturnOnlyUpcomingTickets() {
        // Given
        String userId = "user123";
        LocalDateTime now = LocalDateTime.now();

        Ticket pastTicket = Ticket.builder()
                .userId(userId)
                .routeId(1L)
                .scheduleId(5L)
                .routeName("Past")
                .departureStop("A")
                .arrivalStop("B")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .travelDate(now.minusDays(1))
                .price(BigDecimal.valueOf(25.00))
                .status(TicketStatus.ACTIVE)
                .build();
        ticketRepository.save(pastTicket);

        Ticket futureTicket = Ticket.builder()
                .userId(userId)
                .routeId(1L)
                .scheduleId(5L)
                .routeName("Future")
                .departureStop("A")
                .arrivalStop("B")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .travelDate(now.plusDays(1))
                .price(BigDecimal.valueOf(25.00))
                .status(TicketStatus.ACTIVE)
                .build();
        ticketRepository.save(futureTicket);

        // When
        List<TicketResponse> upcomingTickets = ticketService.getUpcomingTickets(userId);

        // Then
        assertThat(upcomingTickets).hasSize(1);
        assertThat(upcomingTickets.get(0).getRouteName()).isEqualTo("Future");
    }

    // ==================== Tests d'annulation ====================

    @Test
    @DisplayName("Devrait annuler un ticket actif et le sauvegarder")
    void whenCancelTicket_andActive_thenUpdateStatusInDatabase() {
        // Given
        Ticket ticket = Ticket.builder()
                .userId("user123")
                .routeId(1L)
                .scheduleId(5L)
                .routeName("Test Route")
                .departureStop("A")
                .arrivalStop("B")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .travelDate(LocalDateTime.now().plusDays(1))
                .price(BigDecimal.valueOf(25.00))
                .status(TicketStatus.ACTIVE)
                .build();
        Ticket savedTicket = ticketRepository.save(ticket);

        // When
        TicketResponse response = ticketService.cancelTicket(savedTicket.getId(), "user123");

        // Then
        assertThat(response.getStatus()).isEqualTo(TicketStatus.CANCELLED);

        // Vérifier en base
        Ticket updatedTicket = ticketRepository.findById(savedTicket.getId()).orElseThrow();
        assertThat(updatedTicket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
    }

    @Test
    @DisplayName("Devrait rejeter l'annulation d'un ticket non-actif")
    void whenCancelTicket_andNotActive_thenThrowException() {
        // Given
        Ticket ticket = Ticket.builder()
                .userId("user123")
                .routeId(1L)
                .scheduleId(5L)
                .routeName("Test Route")
                .departureStop("A")
                .arrivalStop("B")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .travelDate(LocalDateTime.now().plusDays(1))
                .price(BigDecimal.valueOf(25.00))
                .status(TicketStatus.USED)
                .build();
        Ticket savedTicket = ticketRepository.save(ticket);

        // When & Then
        assertThatThrownBy(() -> ticketService.cancelTicket(savedTicket.getId(), "user123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only active tickets can be cancelled");
    }

    // ==================== Tests d'utilisation ====================

    @Test
    @DisplayName("Devrait marquer un ticket comme utilisé")
    void whenUseTicket_andActive_thenMarkAsUsed() {
        // Given
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);
        Ticket ticket = Ticket.builder()
                .userId("user123")
                .routeId(1L)
                .scheduleId(5L)
                .routeName("Test Route")
                .departureStop("A")
                .arrivalStop("B")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .travelDate(futureDate)
                .price(BigDecimal.valueOf(25.00))
                .status(TicketStatus.ACTIVE)
                .validUntil(futureDate.plusHours(2))
                .build();
        Ticket savedTicket = ticketRepository.save(ticket);

        // When
        TicketResponse response = ticketService.useTicket(savedTicket.getId(), "user123");

        // Then
        assertThat(response.getStatus()).isEqualTo(TicketStatus.USED);

        // Vérifier en base
        Ticket updatedTicket = ticketRepository.findById(savedTicket.getId()).orElseThrow();
        assertThat(updatedTicket.getStatus()).isEqualTo(TicketStatus.USED);
    }

    @Test
    @DisplayName("Devrait rejeter l'utilisation d'un ticket expiré")
    void whenUseTicket_andExpired_thenThrowException() {
        // Given
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        Ticket ticket = Ticket.builder()
                .userId("user123")
                .routeId(1L)
                .scheduleId(5L)
                .routeName("Test Route")
                .departureStop("A")
                .arrivalStop("B")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .travelDate(pastDate)
                .price(BigDecimal.valueOf(25.00))
                .status(TicketStatus.ACTIVE)
                .validUntil(pastDate.plusHours(2))
                .build();
        Ticket savedTicket = ticketRepository.save(ticket);

        // When & Then
        assertThatThrownBy(() -> ticketService.useTicket(savedTicket.getId(), "user123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ticket has expired");

        // Vérifier que le statut a été mis à jour à EXPIRED
        Ticket updatedTicket = ticketRepository.findById(savedTicket.getId()).orElseThrow();
        assertThat(updatedTicket.getStatus()).isEqualTo(TicketStatus.EXPIRED);
    }

    // ==================== Tests de génération de QR Code ====================

    @Test
    @DisplayName("Devrait générer un QR code unique pour chaque ticket")
    void whenCreateMultipleTickets_thenEachHasUniqueQRCode() {
        // When
        TicketResponse ticket1 = ticketService.createTicket(validRequest);

        TicketRequest request2 = TicketRequest.builder()
                .userId("user456")
                .routeId(1L)
                .scheduleId(5L)
                .travelDate(LocalDateTime.now().plusDays(1))
                .price(BigDecimal.valueOf(25.00))
                .build();
        TicketResponse ticket2 = ticketService.createTicket(request2);

        // Then
        Ticket savedTicket1 = ticketRepository.findById(ticket1.getId()).orElseThrow();
        Ticket savedTicket2 = ticketRepository.findById(ticket2.getId()).orElseThrow();

        assertThat(savedTicket1.getQrCode()).isNotNull();
        assertThat(savedTicket2.getQrCode()).isNotNull();
        assertThat(savedTicket1.getQrCode()).isNotEqualTo(savedTicket2.getQrCode());
    }
}

