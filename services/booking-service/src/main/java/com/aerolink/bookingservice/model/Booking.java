package com.aerolink.bookingservice.model;

public class Booking {

    private String bookingId;
    private String userId;
    private String flightId;
    private String passengerName;
    private int seatCount;
    private double totalAmount;
    private String bookingStatus;
    private String paymentStatus;
    private String createdAt;

    public Booking() {
    }

    public Booking(String bookingId, String userId, String flightId, String passengerName,
                   int seatCount, double totalAmount, String bookingStatus,
                   String paymentStatus, String createdAt) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.flightId = flightId;
        this.passengerName = passengerName;
        this.seatCount = seatCount;
        this.totalAmount = totalAmount;
        this.bookingStatus = bookingStatus;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFlightId() {
        return flightId;
    }

    public void setFlightId(String flightId) {
        this.flightId = flightId;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public int getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(int seatCount) {
        this.seatCount = seatCount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
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
}