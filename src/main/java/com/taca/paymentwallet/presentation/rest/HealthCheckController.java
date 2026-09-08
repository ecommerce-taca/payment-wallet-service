package com.taca.paymentwallet.presentation.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthCheckController {

    @GetMapping("/api/v1/payment-wallet/health")
    HealthCheckResponse health() {
        return new HealthCheckResponse("payment-wallet-service", "UP");
    }

    record HealthCheckResponse(String service, String status) {
    }
}
