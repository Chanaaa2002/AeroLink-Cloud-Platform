package com.aerolink.baggageservice.dto;

public class CreateBaggageRequest {

    private String bookingId;
    private String currentLocation;

    public CreateBaggageRequest() {
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }
}