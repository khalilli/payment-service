package com.example.payment.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(

        @NotNull
        UUID sourceAccountId,

        @NotNull
        UUID destinationAccountId,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount

) {
}