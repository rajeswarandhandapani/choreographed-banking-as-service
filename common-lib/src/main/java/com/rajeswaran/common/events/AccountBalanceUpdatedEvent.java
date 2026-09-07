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
public class AccountBalanceUpdatedEvent extends SagaEvent {
    private String paymentId;
    private String description;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private Double amount;
    private String recipientUsername; // Added field to identify the recipient
    private Double sourceAccountBalance;
    private Double destinationAccountBalance;
}
