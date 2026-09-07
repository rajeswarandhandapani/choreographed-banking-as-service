package com.rajeswaran.payment.dto;

import jakarta.validation.constraints.*;
public record CreatePaymentRequest(@NotBlank String sourceAccountNumber, @NotBlank String destinationAccountNumber,
        @Positive double amount, @NotBlank @Size(max=255) String description) {}
