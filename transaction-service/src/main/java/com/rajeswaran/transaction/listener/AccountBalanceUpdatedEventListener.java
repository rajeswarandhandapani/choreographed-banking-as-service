package com.rajeswaran.transaction.listener;

import com.rajeswaran.common.events.AccountBalanceUpdatedEvent;
import com.rajeswaran.common.events.TransactionRecordedEvent;
import com.rajeswaran.common.util.SagaEventBuilderUtil;
import com.rajeswaran.transaction.entity.Transaction;
import com.rajeswaran.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rajeswaran.common.messaging.EventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class AccountBalanceUpdatedEventListener {
    private static final Logger log = LoggerFactory.getLogger(AccountBalanceUpdatedEventListener.class);

    private final com.rajeswaran.transaction.service.TransferLedger ledger;
    private final EventPublisher streamBridge;

    @Bean
    public Consumer<AccountBalanceUpdatedEvent> accountBalanceUpdatedEvent() {
        return event -> {
            log.info("Received AccountBalanceUpdatedEvent for paymentId={}, amount={}", event.getPaymentId(), event.getAmount());

            var entries = ledger.record(event);
            Transaction savedDebitTransaction = entries.debit();
            Transaction savedCreditTransaction = entries.credit();

            // Publish TransactionRecordedEvent for audit
            TransactionRecordedEvent transactionRecordedEvent = TransactionRecordedEvent.builder()
                    .paymentId(event.getPaymentId())
                    .sourceAccountNumber(event.getSourceAccountNumber())
                    .destinationAccountNumber(event.getDestinationAccountNumber())
                    .amount(event.getAmount())
                    .debitTransactionId(String.valueOf(savedDebitTransaction.getId()))
                    .creditTransactionId(String.valueOf(savedCreditTransaction.getId()))
                    .username(event.getUsername())
                    .timestamp(SagaEventBuilderUtil.now())
                    .details("Transactions recorded for paymentId: " + event.getPaymentId())
                    .correlationId(event.getCorrelationId())
                    .serviceName(com.rajeswaran.common.AppConstants.ServiceName.AUDIT_SERVICE)
                    .eventType(com.rajeswaran.common.AppConstants.SagaEventType.TRANSACTION_RECORDED)
                    .build();

            streamBridge.publish("auditEvent-out-0", transactionRecordedEvent);
            log.info("Published TransactionRecordedEvent for paymentId={}", event.getPaymentId());
        };
    }
}
