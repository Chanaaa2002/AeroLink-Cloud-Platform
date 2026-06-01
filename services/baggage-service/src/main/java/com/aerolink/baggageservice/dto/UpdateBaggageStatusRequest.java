package com.aerolink.baggageservice.dto;

public class UpdateBaggageStatusRequest {

    private String status;
    private String currentLocation;

    public UpdateBaggageStatusRequest() {
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
}