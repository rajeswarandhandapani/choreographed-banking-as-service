package com.rajeswaran.transaction.service;
import com.rajeswaran.common.events.AccountBalanceUpdatedEvent;
import com.rajeswaran.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(TransferLedger.class)
class TransferLedgerTest {
    @Autowired TransferLedger ledger;
    @Autowired TransactionRepository transactions;
    @Test void recordsBothSidesAndDescription() {
        var entries = ledger.record(event());
        assertEquals("DEBIT", entries.debit().getType()); assertEquals("CREDIT", entries.credit().getType());
        assertEquals("Lunch", entries.debit().getDescription()); assertEquals("james", entries.credit().getUsername());
        assertEquals(2, transactions.count());
    }
    @Test void duplicateEventDoesNotCreateDuplicateEntries() {
        var first = ledger.record(event()); var second = ledger.record(event());
        assertEquals(first.debit().getId(), second.debit().getId()); assertEquals(2, transactions.count());
    }
    private AccountBalanceUpdatedEvent event() {
        return AccountBalanceUpdatedEvent.builder().paymentId("1").sourceAccountNumber("100").destinationAccountNumber("200")
            .username("john").recipientUsername("james").amount(25.0).sourceAccountBalance(75.0).destinationAccountBalance(45.0).description("Lunch").build();
    }
}
