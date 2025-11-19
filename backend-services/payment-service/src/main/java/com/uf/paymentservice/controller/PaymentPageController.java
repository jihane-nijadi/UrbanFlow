package com.uf.paymentservice.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
public class PaymentPageController {

    @GetMapping(value = "/payments/{ticketId}")
    public ResponseEntity<Void> paymentPage(@PathVariable String ticketId,
                                            @RequestParam(required = false) String returnUrl,
                                            @RequestParam(required = false) String amount) {
        // Avoid serving a raw payment HTML page. Redirect into the frontend payments UI.
        String redirectTo = (returnUrl != null && !returnUrl.isBlank()) ? returnUrl : ("/passenger?session=" + ticketId + (amount != null ? "&amount=" + amount : ""));
        return ResponseEntity.status(302).header("Location", redirectTo).build();
    }
}
