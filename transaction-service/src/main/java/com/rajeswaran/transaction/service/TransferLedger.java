package com.rajeswaran.transaction.service;

import com.rajeswaran.common.events.AccountBalanceUpdatedEvent;
import com.rajeswaran.transaction.entity.Transaction;
import com.rajeswaran.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferLedger {
    private final TransactionRepository transactions;
    public record Entries(Transaction debit, Transaction credit) {}

    @Transactional
    public Entries record(AccountBalanceUpdatedEvent event) {
        var existing = transactions.findByReference(event.getPaymentId());
        if (!existing.isEmpty()) {
            Transaction debit = existing.stream().filter(t -> "DEBIT".equals(t.getType())).findFirst().orElseThrow();
            Transaction credit = existing.stream().filter(t -> "CREDIT".equals(t.getType())).findFirst().orElseThrow();
            return new Entries(debit, credit);
        }
        Transaction debit = entry(event, "DEBIT", event.getSourceAccountNumber(), event.getUsername(), event.getSourceAccountBalance());
        Transaction credit = entry(event, "CREDIT", event.getDestinationAccountNumber(), event.getRecipientUsername(), event.getDestinationAccountBalance());
        transactions.save(debit);
        transactions.save(credit);
        transactions.flush();
        return new Entries(debit, credit);
    }
    private Transaction entry(AccountBalanceUpdatedEvent event, String type, String account, String username, Double balance) {
        if (balance == null || !Double.isFinite(balance)) throw new IllegalArgumentException("An updated balance is required");
        Transaction entry = new Transaction();
        entry.setAccountNumber(account);
        entry.setUsername(username);
        entry.setType(type);
        entry.setAmount(event.getAmount());
        entry.setBalance(balance);
        entry.setStatus("COMPLETED");
        entry.setDescription(event.getDescription() != null ? event.getDescription() : "Payment " + event.getPaymentId());
        entry.setReference(event.getPaymentId());
        entry.setTimestamp(LocalDateTime.now());
        return entry;
    }
}
