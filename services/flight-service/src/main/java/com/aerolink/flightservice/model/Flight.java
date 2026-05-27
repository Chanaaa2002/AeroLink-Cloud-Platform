
package com.aerolink.flightservice.model;

public class Flight {

    private String flightId;
    private String flightNumber;
    private String fromLocation;
    private String toLocation;
    private String departureTime;
    private String arrivalTime;
    private double price;
    private int availableSeats;
    private String status;

    public Flight() {
    }

    public Flight(String flightId, String flightNumber, String fromLocation, String toLocation,
                  String departureTime, String arrivalTime, double price,
                  int availableSeats, String status) {
        this.flightId = flightId;
        this.flightNumber = flightNumber;
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.price = price;
        this.availableSeats = availableSeats;
        this.status = status;
    }

    public String getFlightId() {
        return flightId;
    }

    public void setFlightId(String flightId) {
        this.flightId = flightId;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
