package com.rajeswaran.account.listener;

import com.rajeswaran.account.entity.Account;
import com.rajeswaran.account.service.AccountService;
import com.rajeswaran.common.AppConstants;
import com.rajeswaran.common.events.AccountOpenFailedEvent;
import com.rajeswaran.common.events.AccountOpenedEvent;
import com.rajeswaran.common.events.UserRegisteredEvent;
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
public class UserRegisteredEventListener {
    private final AccountService accountService;
    private final EventPublisher streamBridge;

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredEventListener.class);



    @Bean
    public Consumer<UserRegisteredEvent> userRegisteredEvent() {
        return event -> {
            log.info("Received UserRegisteredEvent for userId={}, username={}, email={}", event.getUserId(), event.getUsername(), event.getEmail());

            // Check if the user has admin role using the dedicated roles field
            if (event.getRoles() != null && event.getRoles().contains(AppConstants.ROLE_BAAS_ADMIN)) {
                log.info("Skipping account creation for admin user: {}", event.getUsername());
                return; // Skip account creation for admin users
            }

            try {
                // Create a new account for the registered user
                Account account = new Account();
                account.setAccountNumber(generateAccountNumber());
                account.setAccountType("SAVINGS");
                account.setUserName(event.getUsername());
                account.setUserId(event.getUserId());
                account.setBalance(500.0);
                account.setStatus("ACTIVE");
                account.setCreatedTimestamp(LocalDateTime.now());
                accountService.createAccount(account);
                log.info("Created new account for userId={}, accountNumber={}", event.getUserId(), account.getAccountNumber());

                AccountOpenedEvent accountOpenedEvent = AccountOpenedEvent.builder()
                        .userId(event.getUserId())
                        .username(event.getUsername())
                        .email(event.getEmail())
                        .fullName(event.getFullName())
                        .timestamp(LocalDateTime.now())
                        .details("Account opened for user: " + event.getUsername())
                        .correlationId(event.getCorrelationId())
                        .serviceName(AppConstants.ServiceName.ACCOUNT_SERVICE)
                        .eventType(AppConstants.SagaEventType.ACCOUNT_OPENED)
                        .accountType(account.getAccountType())
                        .accountNumber(account.getAccountNumber())
                        .balance(account.getBalance())
                        .status(account.getStatus())
                        .build();

                streamBridge.publish("auditEvent-out-0", accountOpenedEvent);
                streamBridge.publish("notificationEvent-out-0", accountOpenedEvent);
            } catch (Exception ex) {
                log.error("Account creation failed for userId={}, username={}, reason={}", event.getUserId(), event.getUsername(), ex.getMessage());
                AccountOpenFailedEvent failedEvent = AccountOpenFailedEvent.builder()
                        .userId(event.getUserId())
                        .username(event.getUsername())
                        .email(event.getEmail())
                        .fullName(event.getFullName())
                        .details(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .correlationId(event.getCorrelationId())
                        .serviceName(AppConstants.ServiceName.ACCOUNT_SERVICE)
                        .eventType(AppConstants.SagaEventType.ACCOUNT_OPEN_FAILED)
                        .build();
                streamBridge.publish("accountOpenFailedEvent-out-0", failedEvent);
                streamBridge.publish("auditEvent-out-0", failedEvent);
            }
        };
    }

    private String generateAccountNumber() {
        String accountNumber = new java.math.BigInteger(java.util.UUID.randomUUID().toString().replace("-", ""), 16).toString();
        log.info("Generated account number: {}", accountNumber);
        return accountNumber;
    }
}
