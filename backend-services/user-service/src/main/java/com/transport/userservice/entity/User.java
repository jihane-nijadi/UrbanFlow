package com.transport.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String firstName;
    private String lastName;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.PASSENGER;

    @Column(updatable = false)
    @Builder.Default  // ← AJOUTEZ CETTE LIGNE
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum UserRole {
        PASSENGER, DRIVER, ADMINISTRATOR
    }
}