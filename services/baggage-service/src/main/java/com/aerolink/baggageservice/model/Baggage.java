package com.aerolink.baggageservice.model;

public class Baggage {

    private String baggageId;
    private String bookingId;
    private String tagNumber;
    private String status;
    private String currentLocation;
    private String lastUpdated;

    public Baggage() {
    }

    public Baggage(String baggageId, String bookingId, String tagNumber,
                   String status, String currentLocation, String lastUpdated) {
        this.baggageId = baggageId;
        this.bookingId = bookingId;
        this.tagNumber = tagNumber;
        this.status = status;
        this.currentLocation = currentLocation;
        this.lastUpdated = lastUpdated;
    }

    public String getBaggageId() {
        return baggageId;
    }

    public void setBaggageId(String baggageId) {
        this.baggageId = baggageId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getTagNumber() {
        return tagNumber;
    }

    public void setTagNumber(String tagNumber) {
        this.tagNumber = tagNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}