package com.example.payment.service;

import com.example.payment.client.AccountServiceClient;
import com.example.payment.client.TransactionServiceClient;
import com.example.payment.dto.payment.CreatePaymentRequest;
import com.example.payment.dto.payment.PaymentResponse;
import com.example.payment.entity.Payment;
import com.example.payment.enums.PaymentStatus;
import com.example.payment.exception.AccountNotFoundException;
import com.example.payment.exception.InsufficientBalanceException;
import com.example.payment.exception.InvalidPaymentException;
import com.example.payment.exception.ResourceNotFoundException;
import com.example.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private TransactionServiceClient transactionServiceClient;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldCreateSuccessfulPayment() {

        UUID sourceAccountId = UUID.randomUUID();
        UUID destinationAccountId = UUID.randomUUID();

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        sourceAccountId,
                        destinationAccountId,
                        new BigDecimal("10.00")
                );

        Payment savedPayment = new Payment();
        savedPayment.setId(UUID.randomUUID());
        savedPayment.setSourceAccountId(sourceAccountId);
        savedPayment.setDestinationAccountId(destinationAccountId);
        savedPayment.setAmount(new BigDecimal("10.00"));
        savedPayment.setCurrency("AZN");
        savedPayment.setStatus(PaymentStatus.PENDING);
        savedPayment.setCreatedAt(Instant.now());

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(savedPayment);

        PaymentResponse response =
                paymentService.createPayment(request);

        assertEquals(
                PaymentStatus.COMPLETED,
                response.status()
        );

        verify(accountServiceClient).withdraw(
                sourceAccountId,
                new BigDecimal("10.00")
        );

        verify(accountServiceClient).deposit(
                destinationAccountId,
                new BigDecimal("10.00")
        );

        verify(transactionServiceClient).createTransferDebit(
                sourceAccountId,
                savedPayment.getId(),
                new BigDecimal("10.00")
        );

        verify(transactionServiceClient).createTransferCredit(
                destinationAccountId,
                savedPayment.getId(),
                new BigDecimal("10.00")
        );
    }

    @Test
    void shouldFailPaymentWhenBalanceIsInsufficient() {

        UUID sourceAccountId = UUID.randomUUID();
        UUID destinationAccountId = UUID.randomUUID();

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        sourceAccountId,
                        destinationAccountId,
                        new BigDecimal("100.00")
                );

        Payment savedPayment = new Payment();
        savedPayment.setId(UUID.randomUUID());
        savedPayment.setSourceAccountId(sourceAccountId);
        savedPayment.setDestinationAccountId(destinationAccountId);
        savedPayment.setAmount(new BigDecimal("100.00"));
        savedPayment.setCurrency("AZN");
        savedPayment.setStatus(PaymentStatus.PENDING);
        savedPayment.setCreatedAt(Instant.now());

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(savedPayment);

        doThrow(new InsufficientBalanceException("Insufficient balance"))
                .when(accountServiceClient)
                .withdraw(sourceAccountId, new BigDecimal("100.00"));

        assertThrows(
                InsufficientBalanceException.class,
                () -> paymentService.createPayment(request)
        );

        assertEquals(
                PaymentStatus.FAILED,
                savedPayment.getStatus()
        );

        verify(accountServiceClient).withdraw(
                sourceAccountId,
                new BigDecimal("100.00")
        );

        verify(accountServiceClient, never()).deposit(
                any(UUID.class),
                any(BigDecimal.class)
        );

        verify(transactionServiceClient, never()).createTransferDebit(
                any(UUID.class),
                any(UUID.class),
                any(BigDecimal.class)
        );

        verify(transactionServiceClient, never()).createTransferCredit(
                any(UUID.class),
                any(UUID.class),
                any(BigDecimal.class)
        );
    }

    @Test
    void shouldCompensateSourceWhenDestinationAccountDoesNotExist() {

        UUID sourceAccountId = UUID.randomUUID();
        UUID destinationAccountId = UUID.randomUUID();

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        sourceAccountId,
                        destinationAccountId,
                        new BigDecimal("50.00")
                );

        Payment savedPayment = new Payment();
        savedPayment.setId(UUID.randomUUID());
        savedPayment.setSourceAccountId(sourceAccountId);
        savedPayment.setDestinationAccountId(destinationAccountId);
        savedPayment.setAmount(new BigDecimal("50.00"));
        savedPayment.setCurrency("AZN");
        savedPayment.setStatus(PaymentStatus.PENDING);
        savedPayment.setCreatedAt(Instant.now());

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(savedPayment);

        doThrow(new AccountNotFoundException("Destination account not found"))
                .when(accountServiceClient)
                .deposit(destinationAccountId, new BigDecimal("50.00"));

        assertThrows(
                AccountNotFoundException.class,
                () -> paymentService.createPayment(request)
        );

        assertEquals(
                PaymentStatus.FAILED,
                savedPayment.getStatus()
        );

        verify(accountServiceClient).withdraw(
                sourceAccountId,
                new BigDecimal("50.00")
        );

        verify(accountServiceClient).deposit(
                destinationAccountId,
                new BigDecimal("50.00")
        );

        verify(accountServiceClient).deposit(
                sourceAccountId,
                new BigDecimal("50.00")
        );

        verify(transactionServiceClient, never()).createTransferDebit(
                any(UUID.class),
                any(UUID.class),
                any(BigDecimal.class)
        );

        verify(transactionServiceClient, never()).createTransferCredit(
                any(UUID.class),
                any(UUID.class),
                any(BigDecimal.class)
        );
    }

    @Test
    void shouldRejectPaymentWhenSourceAndDestinationAreSame() {

        UUID accountId = UUID.randomUUID();

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        accountId,
                        accountId,
                        new BigDecimal("25.00")
                );

        assertThrows(
                InvalidPaymentException.class,
                () -> paymentService.createPayment(request)
        );

        verify(paymentRepository, never()).save(any(Payment.class));

        verifyNoInteractions(accountServiceClient);
        verifyNoInteractions(transactionServiceClient);
    }

    @Test
    void shouldGetPayment() {

        UUID paymentId = UUID.randomUUID();
        UUID sourceAccountId = UUID.randomUUID();
        UUID destinationAccountId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setSourceAccountId(sourceAccountId);
        payment.setDestinationAccountId(destinationAccountId);
        payment.setAmount(new BigDecimal("75.00"));
        payment.setCurrency("AZN");
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCreatedAt(createdAt);

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(payment));

        PaymentResponse response =
                paymentService.getPayment(paymentId);

        assertEquals(paymentId, response.id());
        assertEquals(sourceAccountId, response.sourceAccountId());
        assertEquals(destinationAccountId, response.destinationAccountId());
        assertEquals(new BigDecimal("75.00"), response.amount());
        assertEquals("AZN", response.currency());
        assertEquals(PaymentStatus.COMPLETED, response.status());
        assertEquals(createdAt, response.createdAt());

        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void shouldThrowExceptionWhenPaymentDoesNotExist() {

        UUID paymentId = UUID.randomUUID();

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> paymentService.getPayment(paymentId)
        );

        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void shouldGetAllPayments() {

        Payment firstPayment = new Payment();
        firstPayment.setId(UUID.randomUUID());
        firstPayment.setSourceAccountId(UUID.randomUUID());
        firstPayment.setDestinationAccountId(UUID.randomUUID());
        firstPayment.setAmount(new BigDecimal("25.00"));
        firstPayment.setCurrency("AZN");
        firstPayment.setStatus(PaymentStatus.COMPLETED);
        firstPayment.setCreatedAt(Instant.now());

        Payment secondPayment = new Payment();
        secondPayment.setId(UUID.randomUUID());
        secondPayment.setSourceAccountId(UUID.randomUUID());
        secondPayment.setDestinationAccountId(UUID.randomUUID());
        secondPayment.setAmount(new BigDecimal("40.00"));
        secondPayment.setCurrency("AZN");
        secondPayment.setStatus(PaymentStatus.FAILED);
        secondPayment.setCreatedAt(Instant.now());

        when(paymentRepository.findAll())
                .thenReturn(List.of(firstPayment, secondPayment));

        List<PaymentResponse> responses =
                paymentService.getAllPayments();

        assertEquals(2, responses.size());

        assertEquals(
                firstPayment.getId(),
                responses.get(0).id()
        );

        assertEquals(
                firstPayment.getAmount(),
                responses.get(0).amount()
        );

        assertEquals(
                PaymentStatus.COMPLETED,
                responses.get(0).status()
        );

        assertEquals(
                secondPayment.getId(),
                responses.get(1).id()
        );

        assertEquals(
                secondPayment.getAmount(),
                responses.get(1).amount()
        );

        assertEquals(
                PaymentStatus.FAILED,
                responses.get(1).status()
        );

        verify(paymentRepository).findAll();
    }
}