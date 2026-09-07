package com.rajeswaran.account.listener;

import com.rajeswaran.account.entity.TransferReceipt;
import com.rajeswaran.account.service.AccountTransfers;
import com.rajeswaran.common.events.*;
import com.rajeswaran.common.messaging.EventPublisher;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class PaymentInitiatedEventListenerTest {
    @Test void brokerFailurePropagatesWithoutFalselyDeclaringTransferFailed() {
        AccountTransfers transfers = mock(AccountTransfers.class); EventPublisher publisher = mock(EventPublisher.class);
        var request = PaymentInitiatedEvent.builder().paymentId("1").build();
        when(transfers.transfer(request)).thenReturn(new TransferReceipt());
        doThrow(new IllegalStateException("broker down")).when(publisher).publish(eq("accountBalanceUpdatedEvent-out-0"), any());
        var listener = new PaymentInitiatedEventListener(transfers, publisher).paymentInitiatedEvent();
        assertThrows(IllegalStateException.class, () -> listener.accept(request));
        verify(publisher, never()).publish(eq("paymentFailedEvent-out-0"), any());
    }
    @Test void rejectedTransferPublishesFailure() {
        AccountTransfers transfers = mock(AccountTransfers.class); EventPublisher publisher = mock(EventPublisher.class);
        var request = PaymentInitiatedEvent.builder().paymentId("1").build();
        when(transfers.transfer(request)).thenThrow(new IllegalArgumentException("Insufficient balance"));
        new PaymentInitiatedEventListener(transfers, publisher).paymentInitiatedEvent().accept(request);
        verify(publisher).publish(eq("paymentFailedEvent-out-0"), any(PaymentFailedEvent.class));
        verify(publisher, never()).publish(eq("accountBalanceUpdatedEvent-out-0"), any());
    }
}
