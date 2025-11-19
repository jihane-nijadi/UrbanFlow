package com.uf.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_session")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private Long ticketId;

    private String userId;

    private BigDecimal amount;

    private String status; // PENDING, COMPLETED, FAILED

    private String paymentMethod;

    private String returnUrl;

    @Lob
    private String metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
