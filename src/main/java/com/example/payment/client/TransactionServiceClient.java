package com.example.payment.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionServiceClient {

    private final RestClient transactionServiceRestClient;

    public void createTransferDebit(
            UUID accountId,
            UUID paymentId,
            BigDecimal amount
    ) {
        createTransaction(
                accountId,
                paymentId,
                "TRANSFER_DEBIT",
                amount
        );
    }

    public void createTransferCredit(
            UUID accountId,
            UUID paymentId,
            BigDecimal amount
    ) {
        createTransaction(
                accountId,
                paymentId,
                "TRANSFER_CREDIT",
                amount
        );
    }

    private void createTransaction(
            UUID accountId,
            UUID paymentId,
            String type,
            BigDecimal amount
    ) {
        CreateTransactionRequest request =
                new CreateTransactionRequest(
                        accountId,
                        paymentId,
                        type,
                        amount,
                        "COMPLETED"
                );

        transactionServiceRestClient.post()
                .uri("/api/transactions")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private record CreateTransactionRequest(
            UUID accountId,
            UUID paymentId,
            String type,
            BigDecimal amount,
            String status
    ) {
    }
}