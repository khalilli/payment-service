package com.example.payment.controller;


import com.example.payment.client.AccountServiceClient;
import com.example.payment.dto.payment.CreatePaymentRequest;
import com.example.payment.dto.payment.PaymentResponse;
import com.example.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final AccountServiceClient accountServiceClient;

    @GetMapping("/test-account/{accountId}")
    public String testAccountService(
            @PathVariable UUID accountId
    ) {
        accountServiceClient.deposit(accountId, BigDecimal.ONE);

        return "Account Service call successful";
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@PathVariable UUID paymentId) {
        return paymentService.getPayment(paymentId);
    }
}