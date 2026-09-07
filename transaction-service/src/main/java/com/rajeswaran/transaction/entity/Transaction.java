package com.rajeswaran.transaction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", uniqueConstraints = @UniqueConstraint(columnNames = {"reference", "type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String accountNumber;
    private double amount;
    private String type;
    private String description;
    private String status;
    private String reference;
    private LocalDateTime timestamp;
    private String username; // Added field to track which user owns this transaction
    private double balance; // Renamed from balanceAfterTransaction to balance
}

