package com.rajeswaran.payment.controller;

import com.rajeswaran.common.util.SecurityUtil;
import com.rajeswaran.payment.entity.Payment;
import com.rajeswaran.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PreAuthorize("hasRole(T(com.rajeswaran.common.AppConstants).ROLE_BAAS_ADMIN)")
    @GetMapping
    public List<Payment> getAllPayments() {
        log.info("Received request: getAllPayments");
        List<Payment> payments = paymentService.getAllPayments();
        log.info("Completed request: getAllPayments, count={}", payments.size());
        return payments;
    }

    @PreAuthorize("hasRole(T(com.rajeswaran.common.AppConstants).ROLE_ACCOUNT_HOLDER)")
    @GetMapping("/my-payments")
    public List<Payment> getMyPayments() {
        log.info("Received request: getMyPayments");
        String username = SecurityUtil.getCurrentUsername();
        List<Payment> payments = paymentService.getPaymentsByUsername(username);
        log.info("User {} requesting their payments, count={}", username, payments.size());
        return payments;
    }

    @PreAuthorize("hasRole(T(com.rajeswaran.common.AppConstants).ROLE_BAAS_ADMIN) or hasRole(T(com.rajeswaran.common.AppConstants).ROLE_ACCOUNT_HOLDER)")
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        log.info("Received request: getPaymentById, id={}", id);
        Optional<Payment> payment = paymentService.getPaymentById(id);
        if (payment.isPresent()) {
            log.info("Completed request: getPaymentById, found paymentId={}", id);
            return ResponseEntity.ok(payment.get());
        } else {
            log.info("Completed request: getPaymentById, paymentId={} not found", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole(T(com.rajeswaran.common.AppConstants).ROLE_ACCOUNT_HOLDER)")
    @PostMapping
    public ResponseEntity<Payment> createPayment(@jakarta.validation.Valid @RequestBody com.rajeswaran.payment.dto.CreatePaymentRequest request) {
        Payment created = paymentService.createPayment(request, SecurityUtil.getCurrentUsername());
        return ResponseEntity.accepted().body(created);
    }
}
