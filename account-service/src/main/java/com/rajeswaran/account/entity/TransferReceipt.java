package com.rajeswaran.account.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Persisted in the same transaction as the balances to make redelivery safe. */
@Entity
@Table(name = "transfer_receipts")
@Data
@NoArgsConstructor
public class TransferReceipt {
    @Id private String paymentId;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private String username;
    private String recipientUsername;
    private double amount;
    private double sourceBalance;
    private double destinationBalance;
}
