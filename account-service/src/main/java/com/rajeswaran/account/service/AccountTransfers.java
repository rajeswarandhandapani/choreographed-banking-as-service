package com.rajeswaran.account.service;

import com.rajeswaran.common.events.PaymentInitiatedEvent;
import com.rajeswaran.account.entity.TransferReceipt;

public interface AccountTransfers {
    TransferReceipt transfer(PaymentInitiatedEvent request);
}
