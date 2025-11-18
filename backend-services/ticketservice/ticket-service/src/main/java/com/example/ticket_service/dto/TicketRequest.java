package com.example.ticket_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotNull(message = "Route ID is required")
    private Long routeId;

    @NotNull(message = "Schedule ID is required")
    private Long scheduleId;

    @NotNull(message = "Travel date is required")
    private LocalDateTime travelDate;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;
}
