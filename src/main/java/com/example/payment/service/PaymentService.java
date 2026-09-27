package com.example.payment.service;

import com.example.payment.client.AccountServiceClient;
import com.example.payment.client.TransactionServiceClient;
import com.example.payment.dto.payment.CreatePaymentRequest;
import com.example.payment.dto.payment.PaymentResponse;
import com.example.payment.entity.Payment;
import com.example.payment.enums.PaymentStatus;
import com.example.payment.exception.InvalidPaymentException;
import com.example.payment.exception.ResourceNotFoundException;
import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountServiceClient accountServiceClient;
    private final TransactionServiceClient transactionServiceClient;

    public PaymentResponse createPayment(CreatePaymentRequest request) {

        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new InvalidPaymentException("Source and destination accounts must be different");
        }

        Payment payment = createPaymentEntity(request);

        Payment savedPayment = paymentRepository.save(payment);

        boolean withdrawn = false;

        try {
            accountServiceClient.withdraw(
                    request.sourceAccountId(),
                    request.amount()
            );

            withdrawn = true;

            accountServiceClient.deposit(
                    request.destinationAccountId(),
                    request.amount()
            );

            savedPayment.setStatus(PaymentStatus.COMPLETED);

            transactionServiceClient.createTransferDebit(
                    request.sourceAccountId(),
                    savedPayment.getId(),
                    request.amount()
            );

            transactionServiceClient.createTransferCredit(
                    request.destinationAccountId(),
                    savedPayment.getId(),
                    request.amount()
            );

        } catch (RuntimeException exception) {

            if (withdrawn) {
                accountServiceClient.deposit(
                        request.sourceAccountId(),
                        request.amount()
                );
            }

            savedPayment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(savedPayment);

            throw exception;
        }

        Payment updatedPayment = paymentRepository.save(savedPayment);

        return toPaymentResponse(updatedPayment);
    }

    private Payment createPaymentEntity(CreatePaymentRequest request) {

        Payment payment = new Payment();

        payment.setSourceAccountId(request.sourceAccountId());
        payment.setDestinationAccountId(request.destinationAccountId());
        payment.setAmount(request.amount());
        payment.setCurrency("AZN");
        payment.setStatus(PaymentStatus.PENDING);

        return payment;
    }

    public PaymentResponse getPayment(UUID paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found")
                );

        return toPaymentResponse(payment);
    }

    public List<PaymentResponse> getAllPayments() {

        return paymentRepository.findAll()
                .stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    private PaymentResponse toPaymentResponse(Payment payment) {

        return new PaymentResponse(
                payment.getId(),
                payment.getSourceAccountId(),
                payment.getDestinationAccountId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}