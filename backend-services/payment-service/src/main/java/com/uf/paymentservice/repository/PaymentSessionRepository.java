package com.uf.paymentservice.repository;

import com.uf.paymentservice.entity.PaymentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentSessionRepository extends JpaRepository<PaymentSession, Long> {
    Optional<PaymentSession> findByTicketId(Long ticketId);
}
