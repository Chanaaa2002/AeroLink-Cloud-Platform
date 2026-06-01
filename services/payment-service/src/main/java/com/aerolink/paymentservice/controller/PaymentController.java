package com.aerolink.paymentservice.controller;

import com.aerolink.paymentservice.dto.CreatePaymentRequest;
import com.aerolink.paymentservice.model.Payment;
import com.aerolink.paymentservice.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable String paymentId) {
        return paymentService.getPaymentById(paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestBody CreatePaymentRequest request) {
        Payment createdPayment = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPayment);
    }
}