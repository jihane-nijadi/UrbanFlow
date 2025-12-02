package com.example.ticket_service.integration;

import com.example.ticket_service.dto.RouteDTO;
import com.example.ticket_service.dto.ScheduleDTO;
import com.example.ticket_service.entities.Ticket;
import com.example.ticket_service.entities.TicketStatus;
import com.example.ticket_service.repository.TicketRepository;
import com.example.ticket_service.service.RouteServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration complets pour TicketController
 * Utilise une base de données H2 en mémoire et teste les endpoints REST
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Tests d'intégration - TicketController")
class TicketControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository ticketRepository;

    @MockBean
    private RouteServiceClient routeServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    private RouteDTO mockRoute;
    private ScheduleDTO mockSchedule;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();

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

    // ==================== Tests de création de ticket ====================

    @Test
    @DisplayName("POST /api/tickets - Devrait créer un ticket avec succès")
    void whenCreateTicket_thenReturnCreatedTicket() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("passengerEmail", "user@example.com");
        payload.put("routeId", "1");
        payload.put("scheduleId", "5");
        payload.put("departureTime", LocalDateTime.now().plusDays(1).toString());
        payload.put("price", 25.00);
        payload.put("quantity", 1);

        // When & Then
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value("user@example.com"))
                .andExpect(jsonPath("$.routeId").value(1L))
                .andExpect(jsonPath("$.scheduleId").value(5L))
                .andExpect(jsonPath("$.routeName").value("Maarif - Ain Sebaa"))
                .andExpect(jsonPath("$.departureStop").value("Maarif"))
                .andExpect(jsonPath("$.arrivalStop").value("Casa Port"))
                .andExpect(jsonPath("$.price").value(25.0))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Vérifier que le ticket est bien sauvegardé en base
        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/tickets - Devrait créer un ticket sans routeId/scheduleId (ad-hoc)")
    void whenCreateTicket_withoutRouteScheduleIds_thenCreateAdHocTicket() throws Exception {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("passengerEmail", "user@example.com");
        payload.put("departureStop", "Stop A");
        payload.put("arrivalStop", "Stop B");
        payload.put("departureTime", LocalDateTime.now().plusDays(1).toString());
        payload.put("price", 15.00);

        // When & Then
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.departureStop").value("Stop A"))
                .andExpect(jsonPath("$.arrivalStop").value("Stop B"));

        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/tickets - Devrait rejeter la création si tous les sièges sont pris")
    void whenCreateTicket_andAllSeatsBooked_thenReturnError() throws Exception {
        // Given - Créer 70 tickets pour le même schedule et date
        LocalDateTime travelDate = LocalDateTime.now().plusDays(1);
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

        Map<String, Object> payload = new HashMap<>();
        payload.put("passengerEmail", "newuser@example.com");
        payload.put("routeId", "1");
        payload.put("scheduleId", "5");
        payload.put("departureTime", travelDate.toString());
        payload.put("price", 25.00);

        // When & Then
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        // Vérifier qu'aucun nouveau ticket n'a été créé
        assertThat(ticketRepository.count()).isEqualTo(70);
    }

    // ==================== Tests de récupération de ticket ====================

    @Test
    @DisplayName("GET /api/tickets/{id} - Devrait récupérer un ticket par ID")
    void whenGetTicketById_thenReturnTicket() throws Exception {
        // Given
        Ticket ticket = Ticket.builder()
                .userId("user@example.com")
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

        // When & Then
        mockMvc.perform(get("/api/tickets/{id}", savedTicket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTicket.getId()))
                .andExpect(jsonPath("$.userId").value("user@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/tickets/{id} - Devrait retourner 404 si ticket non trouvé")
    void whenGetTicketById_andNotFound_thenReturn404() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/tickets/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/tickets/user/{userId} - Devrait récupérer tous les tickets d'un utilisateur")
    void whenGetTicketsByUserId_thenReturnUserTickets() throws Exception {
        // Given
        String userId = "user@example.com";
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

        // When & Then
        mockMvc.perform(get("/api/tickets/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[1].userId").value(userId))
                .andExpect(jsonPath("$[2].userId").value(userId));
    }

    @Test
    @DisplayName("GET /api/tickets/user/{userId}/upcoming - Devrait récupérer les tickets à venir")
    void whenGetUpcomingTickets_thenReturnOnlyUpcomingTickets() throws Exception {
        // Given
        String userId = "user@example.com";
        LocalDateTime now = LocalDateTime.now();

        // Ticket passé
        Ticket pastTicket = Ticket.builder()
                .userId(userId)
                .routeId(1L)
                .scheduleId(5L)
                .routeName("Past Route")
                .departureStop("A")
                .arrivalStop("B")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .travelDate(now.minusDays(1))
                .price(BigDecimal.valueOf(25.00))
                .status(TicketStatus.ACTIVE)
                .build();
        ticketRepository.save(pastTicket);

        // Ticket futur
        Ticket futureTicket = Ticket.builder()
                .userId(userId)
                .routeId(1L)
                .scheduleId(5L)
                .routeName("Future Route")
                .departureStop("A")
                .arrivalStop("B")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .travelDate(now.plusDays(1))
                .price(BigDecimal.valueOf(25.00))
                .status(TicketStatus.ACTIVE)
                .build();
        ticketRepository.save(futureTicket);

        // When & Then
        mockMvc.perform(get("/api/tickets/user/{userId}/upcoming", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].routeName").value("Future Route"));
    }

    @Test
    @DisplayName("GET /api/tickets/user/{userId}/active - Devrait récupérer uniquement les tickets actifs")
    void whenGetActiveTickets_thenReturnOnlyActiveTickets() throws Exception {
        // Given
        String userId = "user@example.com";

        Ticket activeTicket = Ticket.builder()
                .userId(userId)
                .routeId(1L)
                .scheduleId(5L)
                .routeName("Active Route")
                .departureStop("A")
                .arrivalStop("B")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .travelDate(LocalDateTime.now().plusDays(1))
                .price(BigDecimal.valueOf(25.00))
                .status(TicketStatus.ACTIVE)
                .build();
        ticketRepository.save(activeTicket);

        Ticket cancelledTicket = Ticket.builder()
                .userId(userId)
                .routeId(1L)
                .scheduleId(5L)
                .routeName("Cancelled Route")
                .departureStop("A")
                .arrivalStop("B")
                .departureTime("08:00")
                .arrivalTime("08:25")
                .travelDate(LocalDateTime.now().plusDays(1))
                .price(BigDecimal.valueOf(25.00))
                .status(TicketStatus.CANCELLED)
                .build();
        ticketRepository.save(cancelledTicket);

        // When & Then
        mockMvc.perform(get("/api/tickets/user/{userId}/active", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].routeName").value("Active Route"));
    }

    // ==================== Tests d'annulation de ticket ====================

    @Test
    @DisplayName("PUT /api/tickets/{id}/cancel - Devrait annuler un ticket actif")
    void whenCancelTicket_andTicketActive_thenCancelSuccessfully() throws Exception {
        // Given
        Ticket ticket = Ticket.builder()
                .userId("user@example.com")
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

        // When & Then
        mockMvc.perform(put("/api/tickets/{id}/cancel", savedTicket.getId())
                        .param("userId", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTicket.getId()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Vérifier en base
        Ticket updatedTicket = ticketRepository.findById(savedTicket.getId()).orElseThrow();
        assertThat(updatedTicket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
    }

    @Test
    @DisplayName("PUT /api/tickets/{id}/cancel - Devrait rejeter l'annulation d'un ticket non-actif")
    void whenCancelTicket_andTicketNotActive_thenReturnError() throws Exception {
        // Given
        Ticket ticket = Ticket.builder()
                .userId("user@example.com")
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
        mockMvc.perform(put("/api/tickets/{id}/cancel", savedTicket.getId())
                        .param("userId", "user@example.com"))
                .andExpect(status().isBadRequest());
    }

    // ==================== Tests d'utilisation de ticket ====================

    @Test
    @DisplayName("PUT /api/tickets/{id}/use - Devrait marquer un ticket comme utilisé")
    void whenUseTicket_andTicketActive_thenMarkAsUsed() throws Exception {
        // Given
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);
        Ticket ticket = Ticket.builder()
                .userId("user@example.com")
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

        // When & Then
        mockMvc.perform(put("/api/tickets/{id}/use", savedTicket.getId())
                        .param("userId", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTicket.getId()))
                .andExpect(jsonPath("$.status").value("USED"));

        // Vérifier en base
        Ticket updatedTicket = ticketRepository.findById(savedTicket.getId()).orElseThrow();
        assertThat(updatedTicket.getStatus()).isEqualTo(TicketStatus.USED);
    }

    @Test
    @DisplayName("PUT /api/tickets/{id}/use - Devrait rejeter l'utilisation d'un ticket expiré")
    void whenUseTicket_andTicketExpired_thenReturnError() throws Exception {
        // Given
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        Ticket ticket = Ticket.builder()
                .userId("user@example.com")
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
        mockMvc.perform(put("/api/tickets/{id}/use", savedTicket.getId())
                        .param("userId", "user@example.com"))
                .andExpect(status().isBadRequest());
    }

    // ==================== Tests de validation ====================

    @Test
    @DisplayName("POST /api/tickets - Devrait valider que schedule appartient à route")
    void whenCreateTicket_andScheduleNotBelongToRoute_thenReturnError() throws Exception {
        // Given
        ScheduleDTO wrongSchedule = ScheduleDTO.builder()
                .id(5L)
                .routeId("R20") // Différent de R10
                .departureStop("Different Stop")
                .arrivalStop("Another Stop")
                .build();

        when(routeServiceClient.getScheduleById(5L)).thenReturn(wrongSchedule);

        Map<String, Object> payload = new HashMap<>();
        payload.put("passengerEmail", "user@example.com");
        payload.put("routeId", "1");
        payload.put("scheduleId", "5");
        payload.put("departureTime", LocalDateTime.now().plusDays(1).toString());
        payload.put("price", 25.00);

        // When & Then
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        assertThat(ticketRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("POST /api/tickets - Devrait créer un ticket même si route service est indisponible")
    void whenCreateTicket_andRouteServiceUnavailable_thenCreateWithFallback() throws Exception {
        // Given
        when(routeServiceClient.getRouteById(anyLong()))
                .thenThrow(new RuntimeException("Service unavailable"));
        when(routeServiceClient.getScheduleById(anyLong()))
                .thenThrow(new RuntimeException("Service unavailable"));

        Map<String, Object> payload = new HashMap<>();
        payload.put("passengerEmail", "user@example.com");
        payload.put("routeId", "1");
        payload.put("scheduleId", "5");
        payload.put("departureStop", "Stop A");
        payload.put("arrivalStop", "Stop B");
        payload.put("departureTime", LocalDateTime.now().plusDays(1).toString());
        payload.put("price", 25.00);

        // When & Then
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        assertThat(ticketRepository.count()).isEqualTo(1);
    }
}

