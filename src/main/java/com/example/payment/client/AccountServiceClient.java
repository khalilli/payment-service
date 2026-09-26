package com.example.payment.client;

import com.example.payment.exception.AccountNotFoundException;
import com.example.payment.exception.AccountServiceException;
import com.example.payment.exception.InsufficientBalanceException;
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
                .onStatus(
                        status -> status.value() == 404,
                        (request, response) -> {
                            throw new AccountNotFoundException("Account not found");
                        }
                )
                .onStatus(
                        status -> status.value() == 422,
                        (request, response) -> {
                            throw new InsufficientBalanceException("Insufficient balance");
                        }
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        (request, response) -> {
                            throw new AccountServiceException("Account Service is unavailable");
                        }
                )
                .toBodilessEntity();
    }

    public void deposit(UUID accountId, BigDecimal amount) {

        accountServiceRestClient.post()
                .uri("/api/accounts/{accountId}/deposit", accountId)
                .body(new MoneyOperationRequest(amount))
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        (request, response) -> {
                            throw new AccountNotFoundException("Account not found");
                        }
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        (request, response) -> {
                            throw new AccountServiceException("Account Service is unavailable");
                        }
                )
                .toBodilessEntity();
    }

    private record MoneyOperationRequest(BigDecimal amount) {
    }
}