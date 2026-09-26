package com.example.payment.service;

import com.example.payment.client.AccountServiceClient;
import com.example.payment.dto.payment.CreatePaymentRequest;
import com.example.payment.dto.payment.PaymentResponse;
import com.example.payment.entity.Payment;
import com.example.payment.enums.PaymentStatus;
import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountServiceClient accountServiceClient;

    public PaymentResponse createPayment(CreatePaymentRequest request) {

        Payment payment = new Payment();

        payment.setSourceAccountId(request.sourceAccountId());
        payment.setDestinationAccountId(request.destinationAccountId());
        payment.setAmount(request.amount());
        payment.setCurrency("AZN");
        payment.setStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);

        try {
            accountServiceClient.withdraw(
                    request.sourceAccountId(),
                    request.amount()
            );

            accountServiceClient.deposit(
                    request.destinationAccountId(),
                    request.amount()
            );

            savedPayment.setStatus(PaymentStatus.COMPLETED);

        } catch (Exception exception) {

            savedPayment.setStatus(PaymentStatus.FAILED);
        }

        Payment updatedPayment = paymentRepository.save(savedPayment);

        return toPaymentResponse(updatedPayment);
    }

    public PaymentResponse getPayment(UUID paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found")
                );

        return toPaymentResponse(payment);
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