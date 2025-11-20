package com.example.ticket_service.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "User ID is required")
    @Column(nullable = false)
    private String userId;

    @NotNull(message = "Route ID is required")
    @Column(nullable = false)
    private Long routeId;

    @NotNull(message = "Schedule ID is required")
    @Column(nullable = false)
    private Long scheduleId;

    // Route information (fetched from Route Service)
    @Column(nullable = false)
    private String routeName;

    @Column(nullable = false)
    private String departureStop;

    @Column(nullable = false)
    private String arrivalStop;

    @Column(nullable = false)
    private String departureTime;  // "08:00"

    @Column(nullable = false)
    private String arrivalTime;    // "08:25"

    @NotNull(message = "Travel date is required")
    @Column(nullable = false)
    private LocalDateTime travelDate;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TicketStatus status = TicketStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime purchaseDate;

    @Column
    private LocalDateTime validUntil;

    @Column(length = 500)
    private String qrCode;

    @Column(length = 64)
    private String bus;

    @PrePersist
    protected void onCreate() {
        purchaseDate = LocalDateTime.now();
        // Set validUntil to travel date + 2 hours after departure
        if (validUntil == null && travelDate != null) {
            validUntil = travelDate.plusHours(2);
        }
    }
}
