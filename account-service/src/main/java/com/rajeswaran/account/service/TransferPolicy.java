package com.rajeswaran.account.service;

import com.rajeswaran.account.entity.Account;
import com.rajeswaran.common.events.PaymentInitiatedEvent;

final class TransferPolicy {
    private TransferPolicy() {}
    static void validate(PaymentInitiatedEvent request) {
        if (request.getPaymentId() == null || request.getPaymentId().isBlank()) throw new IllegalArgumentException("Payment ID is required");
        if (!Double.isFinite(request.getAmount()) || request.getAmount() <= 0) throw new IllegalArgumentException("Amount must be finite and positive");
        String source = request.getSourceAccountNumber(), destination = request.getDestinationAccountNumber();
        if (source == null || source.isBlank() || destination == null || destination.isBlank()) throw new IllegalArgumentException("Both accounts are required");
        if (source.equals(destination)) throw new IllegalArgumentException("Source and destination must differ");
    }
    static void apply(Account source, Account destination, PaymentInitiatedEvent request) {
        if (request.getUsername() == null || !request.getUsername().equals(source.getUserName())) throw new IllegalArgumentException("Source account does not belong to payment initiator");
        if (!"ACTIVE".equals(source.getStatus()) || !"ACTIVE".equals(destination.getStatus())) throw new IllegalArgumentException("Both accounts must be active");
        if (source.getBalance() < request.getAmount()) throw new IllegalArgumentException("Insufficient balance");
        if (!Double.isFinite(destination.getBalance() + request.getAmount())) throw new IllegalArgumentException("Destination balance exceeds supported range");
        source.setBalance(source.getBalance() - request.getAmount());
        destination.setBalance(destination.getBalance() + request.getAmount());
    }
}
