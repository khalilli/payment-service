package com.example.payment.exception;

public class TransactionServiceException extends RuntimeException {
    public TransactionServiceException(String message) {
        super(message);
    }
}
