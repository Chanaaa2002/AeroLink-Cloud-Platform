package com.aerolink.paymentservice.model;

public class Payment {

    private String paymentId;
    private String bookingId;
    private double amount;
    private String currency;
    private String paymentMethod;
    private String paymentStatus;
    private String createdAt;
    private String processedAt;

    public Payment() {
    }

    public Payment(String paymentId, String bookingId, double amount, String currency,
                   String paymentMethod, String paymentStatus, String createdAt,
                   String processedAt) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(String processedAt) {
        this.processedAt = processedAt;
    }
}