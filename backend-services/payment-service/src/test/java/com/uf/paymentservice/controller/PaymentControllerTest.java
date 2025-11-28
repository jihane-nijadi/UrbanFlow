package com.uf.paymentservice.controller;

import com.uf.paymentservice.entity.PaymentSession;
import com.uf.paymentservice.repository.PaymentSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.uf.paymentservice.config.RabbitConfig;

/**
 * Tests unitaires pour PaymentController
 * Teste les endpoints REST pour la gestion des sessions de paiement et des paiements
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController Unit Tests")
class PaymentControllerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PaymentSessionRepository sessionRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private PaymentController paymentController;

    private PaymentSession paymentSession;
    private Map<String, Object> paymentPayload;

    @BeforeEach
    void setUp() {
        paymentSession = PaymentSession.builder()
                .id(1L)
                .ticketId(123L)
                .userId("user-123")
                .amount(new BigDecimal("25.00"))
                .status("PENDING")
                .paymentMethod(null)
                .returnUrl(null)
                .metadata(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        paymentPayload = new HashMap<>();
        paymentPayload.put("userId", "user-123");
        paymentPayload.put("amount", "25.00");
        paymentPayload.put("paymentMethod", "card");
    }

    // ==================== Tests createSession ====================

    @Test
    @DisplayName("Should successfully create payment session from JSON payload")
    void whenCreateSession_withJsonPayload_thenCreateSession() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("passengerEmail", "user@example.com");
        payload.put("price", "30.00");
        payload.put("returnUrl", "http://example.com/return");

        when(request.getParameterMap()).thenReturn(Collections.emptyMap());
        when(sessionRepository.save(any(PaymentSession.class))).thenAnswer(invocation -> {
            PaymentSession session = invocation.getArgument(0);
            session.setId(1L);
            return session;
        });

        // When
        ResponseEntity<?> response = paymentController.createSession(payload, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("sessionId")).isNotNull();
        assertThat(body.get("amount")).isNotNull();

        verify(sessionRepository, times(1)).save(any(PaymentSession.class));
    }

    @Test
    @DisplayName("Should create session with userId from passengerEmail")
    void whenCreateSession_withPassengerEmail_thenUseAsUserId() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("passengerEmail", "test@example.com");
        payload.put("price", "25.00");

        when(request.getParameterMap()).thenReturn(Collections.emptyMap());
        when(sessionRepository.save(any(PaymentSession.class))).thenAnswer(invocation -> {
            PaymentSession session = invocation.getArgument(0);
            session.setId(1L);
            return session;
        });

        // When
        paymentController.createSession(payload, request);

        // Then
        ArgumentCaptor<PaymentSession> sessionCaptor = ArgumentCaptor.forClass(PaymentSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getUserId()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should create session with default userId when not provided")
    void whenCreateSession_withoutUserId_thenUseDefault() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("price", "25.00");

        when(request.getParameterMap()).thenReturn(Collections.emptyMap());
        when(sessionRepository.save(any(PaymentSession.class))).thenAnswer(invocation -> {
            PaymentSession session = invocation.getArgument(0);
            session.setId(1L);
            return session;
        });

        // When
        paymentController.createSession(payload, request);

        // Then
        ArgumentCaptor<PaymentSession> sessionCaptor = ArgumentCaptor.forClass(PaymentSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getUserId()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Should handle exception when creating session fails")
    void whenCreateSession_withException_thenReturnError() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        when(request.getParameterMap()).thenReturn(Collections.emptyMap());
        when(sessionRepository.save(any(PaymentSession.class))).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = paymentController.createSession(payload, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("error")).isEqualTo("session_creation_failed");
    }

    // ==================== Tests getSession ====================

    @Test
    @DisplayName("Should successfully retrieve payment session by ID")
    void whenGetSession_withValidId_thenReturnSession() {
        // Given
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(paymentSession));

        // When
        ResponseEntity<?> response = paymentController.getSession(1L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(paymentSession);
        verify(sessionRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should return 404 when session not found")
    void whenGetSession_withInvalidId_thenReturnNotFound() {
        // Given
        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = paymentController.getSession(999L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("error")).isEqualTo("session_not_found");
    }

    @Test
    @DisplayName("Should handle exception when fetching session fails")
    void whenGetSession_withException_thenReturnError() {
        // Given
        when(sessionRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<?> response = paymentController.getSession(1L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("error")).isEqualTo("session_lookup_failed");
    }

    // ==================== Tests getCompletedSessionsForUser ====================

    @Test
    @DisplayName("Should successfully retrieve completed sessions for user")
    void whenGetCompletedSessionsForUser_withValidUser_thenReturnSessions() {
        // Given
        PaymentSession completedSession = PaymentSession.builder()
                .id(2L)
                .ticketId(456L)
                .userId("user-123")
                .amount(new BigDecimal("30.00"))
                .status("COMPLETED")
                .paymentMethod("card")
                .build();

        when(sessionRepository.findByUserIdAndStatus("user-123", "COMPLETED"))
                .thenReturn(Arrays.asList(completedSession));

        // When
        ResponseEntity<?> response = paymentController.getCompletedSessionsForUser("user-123");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(List.class);
        
        @SuppressWarnings("unchecked")
        List<PaymentSession> sessions = (List<PaymentSession>) response.getBody();
        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Should return empty list when user has no completed sessions")
    void whenGetCompletedSessionsForUser_withNoSessions_thenReturnEmptyList() {
        // Given
        when(sessionRepository.findByUserIdAndStatus("user-123", "COMPLETED"))
                .thenReturn(Collections.emptyList());

        // When
        ResponseEntity<?> response = paymentController.getCompletedSessionsForUser("user-123");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(List.class);
        
        @SuppressWarnings("unchecked")
        List<PaymentSession> sessions = (List<PaymentSession>) response.getBody();
        assertThat(sessions).isEmpty();
    }

    // ==================== Tests payTicket ====================

    @Test
    @DisplayName("Should successfully process payment for ticket")
    void whenPayTicket_withValidPayload_thenProcessPayment() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", "user-123");
        payload.put("amount", "25.00");
        payload.put("paymentMethod", "card");

        when(request.getParameterMap()).thenReturn(Collections.emptyMap());
        when(sessionRepository.findByTicketId(123L)).thenReturn(Optional.of(paymentSession));
        when(sessionRepository.save(any(PaymentSession.class))).thenReturn(paymentSession);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(createMockTicketResponse());

        // When
        ResponseEntity<?> response = paymentController.payTicket("123", payload, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("status")).isEqualTo("SUCCESS");
        assertThat(body.get("transactionId")).isNotNull();
        assertThat(body.get("ticketId")).isEqualTo("123");

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitConfig.PAYMENT_EXCHANGE), 
                eq(RabbitConfig.PAYMENT_COMPLETED_ROUTING), 
                any(Object.class));
        verify(sessionRepository, times(1)).save(any(PaymentSession.class));
    }

    @Test
    @DisplayName("Should update existing session status to COMPLETED")
    void whenPayTicket_withExistingSession_thenUpdateStatus() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", "user-123");
        payload.put("amount", "25.00");
        payload.put("paymentMethod", "card");

        when(request.getParameterMap()).thenReturn(Collections.emptyMap());
        when(sessionRepository.findByTicketId(123L)).thenReturn(Optional.of(paymentSession));
        when(sessionRepository.save(any(PaymentSession.class))).thenReturn(paymentSession);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(createMockTicketResponse());

        // When
        paymentController.payTicket("123", payload, request);

        // Then
        ArgumentCaptor<PaymentSession> sessionCaptor = ArgumentCaptor.forClass(PaymentSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo("COMPLETED");
        assertThat(sessionCaptor.getValue().getPaymentMethod()).isEqualTo("card");
    }

    @Test
    @DisplayName("Should generate PDF and return in response")
    void whenPayTicket_thenGeneratePDF() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", "user-123");
        payload.put("amount", "25.00");

        when(request.getParameterMap()).thenReturn(Collections.emptyMap());
        when(sessionRepository.findByTicketId(123L)).thenReturn(Optional.of(paymentSession));
        when(sessionRepository.save(any(PaymentSession.class))).thenReturn(paymentSession);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(createMockTicketResponse());

        // When
        ResponseEntity<?> response = paymentController.payTicket("123", payload, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("pdfBase64")).isNotNull();
    }

    @Test
    @DisplayName("Should redirect to returnUrl when provided")
    void whenPayTicket_withReturnUrl_thenRedirect() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", "user-123");
        payload.put("amount", "25.00");
        payload.put("returnUrl", "http://example.com/success");

        when(request.getParameterMap()).thenReturn(Collections.emptyMap());
        when(sessionRepository.findByTicketId(123L)).thenReturn(Optional.of(paymentSession));
        when(sessionRepository.save(any(PaymentSession.class))).thenReturn(paymentSession);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(createMockTicketResponse());

        // When
        ResponseEntity<?> response = paymentController.payTicket("123", payload, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).isNotNull();
    }

    @Test
    @DisplayName("Should handle exception when ticket service is unavailable")
    void whenPayTicket_withTicketServiceError_thenHandleGracefully() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", "user-123");
        payload.put("amount", "25.00");

        when(request.getParameterMap()).thenReturn(Collections.emptyMap());
        when(sessionRepository.findByTicketId(123L)).thenReturn(Optional.of(paymentSession));
        when(sessionRepository.save(any(PaymentSession.class))).thenReturn(paymentSession);
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("Ticket service unavailable"));

        // When
        ResponseEntity<?> response = paymentController.payTicket("123", payload, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("status")).isEqualTo("SUCCESS");
        assertThat(body.get("pdfBase64")).isNull();
    }

    @Test
    @DisplayName("Should handle exception when payment processing fails")
    void whenPayTicket_withException_thenReturnError() {
        // Given - Exception in outer try-catch (e.g., parsing ticketId fails)
        Map<String, Object> payload = new HashMap<>();
        when(request.getParameterMap()).thenThrow(new RuntimeException("Request error"));

        // When
        ResponseEntity<?> response = paymentController.payTicket("123", payload, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("error")).isEqualTo("payment_failed");
    }

    @Test
    @DisplayName("Should use default payment method when not provided")
    void whenPayTicket_withoutPaymentMethod_thenUseDefault() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", "user-123");
        payload.put("amount", "25.00");

        when(request.getParameterMap()).thenReturn(Collections.emptyMap());
        when(sessionRepository.findByTicketId(123L)).thenReturn(Optional.of(paymentSession));
        when(sessionRepository.save(any(PaymentSession.class))).thenReturn(paymentSession);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(createMockTicketResponse());

        // When
        paymentController.payTicket("123", payload, request);

        // Then
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                eq(RabbitConfig.PAYMENT_EXCHANGE), 
                eq(RabbitConfig.PAYMENT_COMPLETED_ROUTING), 
                eventCaptor.capture());
        // The event should have paymentMethod as "card_simulated" (default)
    }

    @Test
    @DisplayName("Should use passengerEmail as userId when userId not provided")
    void whenPayTicket_withPassengerEmail_thenUseAsUserId() {
        // Given
        Map<String, Object> payload = new HashMap<>();
        payload.put("passengerEmail", "test@example.com");
        payload.put("amount", "25.00");

        when(request.getParameterMap()).thenReturn(Collections.emptyMap());
        when(sessionRepository.findByTicketId(123L)).thenReturn(Optional.of(paymentSession));
        when(sessionRepository.save(any(PaymentSession.class))).thenReturn(paymentSession);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(createMockTicketResponse());

        // When
        paymentController.payTicket("123", payload, request);

        // Then
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                eq(RabbitConfig.PAYMENT_EXCHANGE), 
                eq(RabbitConfig.PAYMENT_COMPLETED_ROUTING), 
                eventCaptor.capture());
        // Event should have userId as "test@example.com"
    }

    // ==================== Helper Methods ====================

    private Map<String, Object> createMockTicketResponse() {
        Map<String, Object> ticket = new HashMap<>();
        ticket.put("id", 123L);
        ticket.put("userId", "user-123");
        ticket.put("price", "25.00");
        return ticket;
    }
}
