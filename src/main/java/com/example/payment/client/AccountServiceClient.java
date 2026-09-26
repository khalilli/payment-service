package com.example.payment.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountServiceClient {

    private final RestClient accountServiceRestClient;

    public void withdraw(UUID accountId, BigDecimal amount) {

        accountServiceRestClient.post()
                .uri("/api/accounts/{accountId}/withdraw", accountId)
                .body(new MoneyOperationRequest(amount))
                .retrieve()
                .toBodilessEntity();
    }

    public void deposit(UUID accountId, BigDecimal amount) {

        accountServiceRestClient.post()
                .uri("/api/accounts/{accountId}/deposit", accountId)
                .body(new MoneyOperationRequest(amount))
                .retrieve()
                .toBodilessEntity();
    }

    private record MoneyOperationRequest(BigDecimal amount) {
    }
}