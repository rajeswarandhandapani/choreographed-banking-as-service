package com.rajeswaran.common.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PaymentInitiatedEvent extends SagaEvent {
    private String paymentId;
    private String description;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private double amount;
    private String currency;
}
