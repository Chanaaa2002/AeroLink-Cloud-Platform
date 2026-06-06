package com.aerolink.baggageservice.controller;

import com.aerolink.baggageservice.dto.CreateBaggageRequest;
import com.aerolink.baggageservice.dto.UpdateBaggageStatusRequest;
import com.aerolink.baggageservice.model.Baggage;
import com.aerolink.baggageservice.service.BaggageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaggageControllerTest {

    private final BaggageService baggageService = mock(BaggageService.class);
    private final BaggageController baggageController = new BaggageController(baggageService);

    @Test
    void createBaggageUsesJwtToken() {
        Jwt jwt = jwt("staff-1", List.of("STAFF"), "staff-token");
        CreateBaggageRequest request = new CreateBaggageRequest();
        request.setBookingId("BK-1");

        Baggage baggage = sampleBaggage("BAG-1", "BK-1", "user-1", "CHECKED_IN");
        when(baggageService.createBaggage(request, "staff-token")).thenReturn(baggage);

        ResponseEntity<Baggage> response = baggageController.createBaggage(request, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(baggage);
        verify(baggageService).createBaggage(request, "staff-token");
    }

    @Test
    void getBaggageByBookingIdReturnsOkForStaff() {
        Jwt jwt = jwt("staff-1", List.of("STAFF"), "staff-token");
        Baggage baggage = sampleBaggage("BAG-1", "BK-1", "user-1", "CHECKED_IN");
        when(baggageService.getBaggageByBookingId("BK-1", "staff-token")).thenReturn(List.of(baggage));

        ResponseEntity<List<Baggage>> response = baggageController.getBaggageByBookingId("BK-1", jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(baggage);
    }

    @Test
    void getBaggageByBookingIdReturnsForbiddenWhenItemsDoNotBelongToPassenger() {
        Jwt jwt = jwt("user-1", List.of("PASSENGER"), "passenger-token");
        Baggage baggage = sampleBaggage("BAG-1", "BK-1", "user-2", "CHECKED_IN");
        when(baggageService.getBaggageByBookingId("BK-1", "passenger-token")).thenReturn(List.of(baggage));

        ResponseEntity<List<Baggage>> response = baggageController.getBaggageByBookingId("BK-1", jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getBaggageByIdReturnsOkForOwner() {
        Jwt jwt = jwt("user-1", List.of("PASSENGER"), "passenger-token");
        Baggage baggage = sampleBaggage("BAG-1", "BK-1", "user-1", "CHECKED_IN");
        when(baggageService.getBaggageById("BAG-1")).thenReturn(Optional.of(baggage));

        ResponseEntity<Baggage> response = baggageController.getBaggageById("BAG-1", jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(baggage);
    }

    @Test
    void getBaggageByIdReturnsForbiddenForNonOwnerPassenger() {
        Jwt jwt = jwt("user-1", List.of("PASSENGER"), "passenger-token");
        Baggage baggage = sampleBaggage("BAG-1", "BK-1", "user-2", "CHECKED_IN");
        when(baggageService.getBaggageById("BAG-1")).thenReturn(Optional.of(baggage));

        ResponseEntity<Baggage> response = baggageController.getBaggageById("BAG-1", jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getBaggageByIdReturnsNotFoundWhenMissing() {
        Jwt jwt = jwt("user-1", List.of("PASSENGER"), "passenger-token");
        when(baggageService.getBaggageById("BAG-1")).thenReturn(Optional.empty());

        ResponseEntity<Baggage> response = baggageController.getBaggageById("BAG-1", jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateBaggageStatusDelegatesToService() {
        UpdateBaggageStatusRequest request = new UpdateBaggageStatusRequest();
        request.setStatus("LOADED");
        request.setCurrentLocation("Loading Bay");
        Baggage baggage = sampleBaggage("BAG-1", "BK-1", "user-1", "LOADED");
        when(baggageService.updateBaggageStatus("BAG-1", request)).thenReturn(baggage);

        ResponseEntity<Baggage> response = baggageController.updateBaggageStatus("BAG-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(baggage);
    }

    private Jwt jwt(String subject, List<String> groups, String tokenValue) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(subject);
        when(jwt.getClaimAsStringList("cognito:groups")).thenReturn(groups);
        when(jwt.getTokenValue()).thenReturn(tokenValue);
        return jwt;
    }

    private Baggage sampleBaggage(String baggageId, String bookingId, String userId, String status) {
        Baggage baggage = new Baggage();
        baggage.setBaggageId(baggageId);
        baggage.setBookingId(bookingId);
        baggage.setUserId(userId);
        baggage.setTagNumber("TAG-1");
        baggage.setStatus(status);
        baggage.setCurrentLocation("Check-in Counter");
        baggage.setLastUpdated("2026-06-05T08:00:00");
        return baggage;
    }
}