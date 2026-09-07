package com.rajeswaran.account.service;

import com.rajeswaran.account.entity.Account;
import com.rajeswaran.account.repository.AccountRepository;
import com.rajeswaran.account.repository.TransferReceiptRepository;
import com.rajeswaran.common.events.PaymentInitiatedEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(TransactionalAccountTransfers.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AccountTransfersTest {
    @Autowired AccountRepository accounts;
    @Autowired TransferReceiptRepository receipts;
    @Autowired AccountTransfers transfers;
    @BeforeEach void seed() {
        receipts.deleteAll(); accounts.deleteAll();
        accounts.save(account("100", "john", 100)); accounts.save(account("200", "james", 20));
    }
    @Test void transfersBothBalances() {
        var result = transfers.transfer(request("1", "100", "200", 25));
        assertEquals(75, result.getSourceBalance()); assertEquals(45, result.getDestinationBalance());
        assertEquals(75, balance("100")); assertEquals(45, balance("200"));
        assertEquals(1, receipts.count());
    }
    @Test void replayDoesNotTransferTwice() {
        transfers.transfer(request("1", "100", "200", 25));
        transfers.transfer(request("1", "100", "200", 25));
        assertEquals(75, balance("100")); assertEquals(45, balance("200")); assertEquals(1, receipts.count());
    }
    @Test void changedReplayIsRejected() {
        transfers.transfer(request("1", "100", "200", 25));
        assertThrows(IllegalArgumentException.class, () -> transfers.transfer(request("1", "100", "200", 10)));
        assertEquals(75, balance("100"));
    }
    @ParameterizedTest @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsInvalidAmounts(double amount) {
        assertThrows(IllegalArgumentException.class, () -> transfers.transfer(request("1", "100", "200", amount)));
        assertEquals(100, balance("100")); assertEquals(20, balance("200")); assertEquals(0, receipts.count());
    }
    @Test void missingDestinationDoesNotDebit() {
        assertThrows(IllegalArgumentException.class, () -> transfers.transfer(request("1", "100", "missing", 25)));
        assertEquals(100, balance("100")); assertEquals(0, receipts.count());
    }
    @Test void insufficientBalanceDoesNotCredit() {
        assertThrows(IllegalArgumentException.class, () -> transfers.transfer(request("1", "100", "200", 101)));
        assertEquals(100, balance("100")); assertEquals(20, balance("200"));
    }
    @Test void rejectsSameAccount() {
        assertThrows(IllegalArgumentException.class, () -> transfers.transfer(request("1", "100", "100", 25)));
        assertEquals(100, balance("100"));
    }
    @Test void rejectsUnownedSource() {
        var request = request("1", "100", "200", 25); request.setUsername("attacker");
        assertThrows(IllegalArgumentException.class, () -> transfers.transfer(request)); assertEquals(100, balance("100"));
    }
    @Test void rejectsClosedDestination() {
        var a = accounts.findByAccountNumber("200").orElseThrow(); a.setStatus("CLOSED"); accounts.saveAndFlush(a);
        assertThrows(IllegalArgumentException.class, () -> transfers.transfer(request("1", "100", "200", 25)));
        assertEquals(100, balance("100"));
    }
    @Test void concurrentDuplicateDeliveryMovesMoneyOnce() throws Exception {
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var start = new java.util.concurrent.CountDownLatch(1);
            var a = pool.submit(() -> { start.await(); return transfers.transfer(request("1", "100", "200", 25)); });
            var b = pool.submit(() -> { start.await(); return transfers.transfer(request("1", "100", "200", 25)); });
            start.countDown(); a.get(10, java.util.concurrent.TimeUnit.SECONDS); b.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertEquals(75, balance("100")); assertEquals(45, balance("200")); assertEquals(1, receipts.count());
    }
    private double balance(String number) { return accounts.findByAccountNumber(number).orElseThrow().getBalance(); }
    private Account account(String number, String owner, double balance) {
        Account a = new Account(); a.setAccountNumber(number); a.setUserName(owner); a.setBalance(balance); a.setStatus("ACTIVE"); return a;
    }
    private PaymentInitiatedEvent request(String id, String source, String dest, double amount) {
        return PaymentInitiatedEvent.builder().paymentId(id).sourceAccountNumber(source).destinationAccountNumber(dest).amount(amount).username("john").build();
    }
}
