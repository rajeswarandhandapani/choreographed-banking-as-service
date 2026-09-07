package com.rajeswaran.account.listener;

import com.rajeswaran.account.entity.TransferReceipt;
import com.rajeswaran.account.service.AccountTransfers;
import com.rajeswaran.common.AppConstants;
import com.rajeswaran.common.events.AccountBalanceUpdatedEvent;
import com.rajeswaran.common.events.PaymentFailedEvent;
import com.rajeswaran.common.events.PaymentInitiatedEvent;
import com.rajeswaran.common.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class PaymentInitiatedEventListener {
    private final AccountTransfers transfers;
    private final EventPublisher events;

    @Bean
    public Consumer<PaymentInitiatedEvent> paymentInitiatedEvent() {
        return request -> {
            TransferReceipt result;
            try {
                result = transfers.transfer(request);
            } catch (IllegalArgumentException rejected) {
                var failed = PaymentFailedEvent.builder().paymentId(request.getPaymentId())
                        .username(request.getUsername()).timestamp(LocalDateTime.now()).details(rejected.getMessage())
                        .correlationId(request.getCorrelationId()).serviceName(AppConstants.ServiceName.ACCOUNT_SERVICE)
                        .eventType(AppConstants.SagaEventType.PAYMENT_FAILED).build();
                events.publish("paymentFailedEvent-out-0", failed);
                events.publish("auditEvent-out-0", failed);
                return;
            }
            // Publishing is outside the business-error catch. Failures are retried by the binder;
            // the durable receipt prevents replay from moving the money a second time.
            var updated = AccountBalanceUpdatedEvent.builder().paymentId(request.getPaymentId())
                    .sourceAccountNumber(result.getSourceAccountNumber()).destinationAccountNumber(result.getDestinationAccountNumber())
                    .amount(result.getAmount()).username(result.getUsername()).recipientUsername(result.getRecipientUsername())
                    .sourceAccountBalance(result.getSourceBalance()).destinationAccountBalance(result.getDestinationBalance())
                    .description(request.getDescription()).userId(request.getUserId()).timestamp(LocalDateTime.now())
                    .details("Balances updated for paymentId: " + request.getPaymentId()).correlationId(request.getCorrelationId())
                    .serviceName(AppConstants.ServiceName.ACCOUNT_SERVICE).eventType(AppConstants.SagaEventType.ACCOUNT_BALANCE_UPDATED).build();
            events.publish("accountBalanceUpdatedEvent-out-0", updated);
            events.publish("auditEvent-out-0", updated);
        };
    }
}
