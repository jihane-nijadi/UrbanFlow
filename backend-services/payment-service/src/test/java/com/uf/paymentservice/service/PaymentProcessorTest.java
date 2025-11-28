package com.uf.paymentservice.service;

import com.uf.paymentservice.dto.TicketCreatedEvent;
import com.uf.paymentservice.entity.PaymentSession;
import com.uf.paymentservice.repository.PaymentSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour PaymentProcessor
 * Teste le traitement des événements de tickets créés et la création de sessions de paiement
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentProcessor Unit Tests")
class PaymentProcessorTest {

    @Mock
    private PaymentSessionRepository sessionRepository;

    @InjectMocks
    private PaymentProcessor paymentProcessor;

    private TicketCreatedEvent ticketEvent;
    private PaymentSession existingSession;

    @BeforeEach
    void setUp() {
        ticketEvent = TicketCreatedEvent.builder()
                .ticketId(123L)
                .userId("user-123")
                .routeId("R10")
                .scheduleId("S5")
                .routeName("Maarif - Ain Sebaa")
                .departureStop("Maarif")
                .arrivalStop("Ain Sebaa")
                .price(new BigDecimal("25.00"))
                .build();

        existingSession = PaymentSession.builder()
                .id(1L)
                .ticketId(123L)
                .userId("user-123")
                .amount(new BigDecimal("25.00"))
                .status("PENDING")
                .build();
    }

    // ==================== Tests onTicketCreated ====================

    @Test
    @DisplayName("Should create payment session when ticket is created")
    void whenOnTicketCreated_withNewTicket_thenCreateSession() {
        // Given
        when(sessionRepository.findByTicketId(123L)).thenReturn(Optional.empty());
        when(sessionRepository.save(any(PaymentSession.class))).thenAnswer(invocation -> {
            PaymentSession session = invocation.getArgument(0);
            session.setId(1L);
            return session;
        });

        // When
        paymentProcessor.onTicketCreated(ticketEvent);

        // Then
        ArgumentCaptor<PaymentSession> sessionCaptor = ArgumentCaptor.forClass(PaymentSession.class);
        verify(sessionRepository, times(1)).findByTicketId(123L);
        verify(sessionRepository, times(1)).save(sessionCaptor.capture());

        PaymentSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getTicketId()).isEqualTo(123L);
        assertThat(savedSession.getUserId()).isEqualTo("user-123");
        assertThat(savedSession.getAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(savedSession.getStatus()).isEqualTo("PENDING");
        assertThat(savedSession.getPaymentMethod()).isNull();
        assertThat(savedSession.getReturnUrl()).isNull();
        assertThat(savedSession.getMetadata()).isNull();
    }

    @Test
    @DisplayName("Should not create duplicate session when session already exists")
    void whenOnTicketCreated_withExistingSession_thenSkipCreation() {
        // Given
        when(sessionRepository.findByTicketId(123L)).thenReturn(Optional.of(existingSession));

        // When
        paymentProcessor.onTicketCreated(ticketEvent);

        // Then
        verify(sessionRepository, times(1)).findByTicketId(123L);
        verify(sessionRepository, never()).save(any(PaymentSession.class));
    }

    @Test
    @DisplayName("Should handle exception gracefully when repository fails")
    void whenOnTicketCreated_withRepositoryError_thenHandleGracefully() {
        // Given
        when(sessionRepository.findByTicketId(123L)).thenThrow(new RuntimeException("Database error"));

        // When & Then - Should not throw exception
        assertThatCode(() -> paymentProcessor.onTicketCreated(ticketEvent))
                .doesNotThrowAnyException();

        verify(sessionRepository, times(1)).findByTicketId(123L);
        verify(sessionRepository, never()).save(any(PaymentSession.class));
    }

    @Test
    @DisplayName("Should set correct amount from ticket event")
    void whenOnTicketCreated_thenSetCorrectAmount() {
        // Given
        ticketEvent.setPrice(new BigDecimal("50.75"));
        when(sessionRepository.findByTicketId(123L)).thenReturn(Optional.empty());
        when(sessionRepository.save(any(PaymentSession.class))).thenAnswer(invocation -> {
            PaymentSession session = invocation.getArgument(0);
            session.setId(1L);
            return session;
        });

        // When
        paymentProcessor.onTicketCreated(ticketEvent);

        // Then
        ArgumentCaptor<PaymentSession> sessionCaptor = ArgumentCaptor.forClass(PaymentSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("50.75"));
    }

    @Test
    @DisplayName("Should set correct userId from ticket event")
    void whenOnTicketCreated_thenSetCorrectUserId() {
        // Given
        ticketEvent.setUserId("different-user");
        when(sessionRepository.findByTicketId(123L)).thenReturn(Optional.empty());
        when(sessionRepository.save(any(PaymentSession.class))).thenAnswer(invocation -> {
            PaymentSession session = invocation.getArgument(0);
            session.setId(1L);
            return session;
        });

        // When
        paymentProcessor.onTicketCreated(ticketEvent);

        // Then
        ArgumentCaptor<PaymentSession> sessionCaptor = ArgumentCaptor.forClass(PaymentSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getUserId()).isEqualTo("different-user");
    }

    @Test
    @DisplayName("Should set createdAt and updatedAt timestamps")
    void whenOnTicketCreated_thenSetTimestamps() {
        // Given
        when(sessionRepository.findByTicketId(123L)).thenReturn(Optional.empty());
        when(sessionRepository.save(any(PaymentSession.class))).thenAnswer(invocation -> {
            PaymentSession session = invocation.getArgument(0);
            session.setId(1L);
            return session;
        });

        // When
        paymentProcessor.onTicketCreated(ticketEvent);

        // Then
        ArgumentCaptor<PaymentSession> sessionCaptor = ArgumentCaptor.forClass(PaymentSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        PaymentSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getCreatedAt()).isNotNull();
        assertThat(savedSession.getUpdatedAt()).isNotNull();
    }

    // ==================== Tests getPending ====================

    @Test
    @DisplayName("Should return payment session when it exists")
    void whenGetPending_withExistingSession_thenReturnSession() {
        // Given
        when(sessionRepository.findByTicketId(123L)).thenReturn(Optional.of(existingSession));

        // When
        PaymentSession result = paymentProcessor.getPending(123L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTicketId()).isEqualTo(123L);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(sessionRepository, times(1)).findByTicketId(123L);
    }

    @Test
    @DisplayName("Should return null when session does not exist")
    void whenGetPending_withNonExistentSession_thenReturnNull() {
        // Given
        when(sessionRepository.findByTicketId(123L)).thenReturn(Optional.empty());

        // When
        PaymentSession result = paymentProcessor.getPending(123L);

        // Then
        assertThat(result).isNull();
        verify(sessionRepository, times(1)).findByTicketId(123L);
    }

    @Test
    @DisplayName("Should return null for null ticketId")
    void whenGetPending_withNullTicketId_thenReturnNull() {
        // Given
        when(sessionRepository.findByTicketId(null)).thenReturn(Optional.empty());

        // When
        PaymentSession result = paymentProcessor.getPending(null);

        // Then
        assertThat(result).isNull();
    }
}
