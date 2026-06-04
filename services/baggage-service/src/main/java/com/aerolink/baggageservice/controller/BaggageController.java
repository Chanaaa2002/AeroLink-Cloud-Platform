package com.aerolink.baggageservice.controller;

import com.aerolink.baggageservice.dto.CreateBaggageRequest;
import com.aerolink.baggageservice.dto.UpdateBaggageStatusRequest;
import com.aerolink.baggageservice.model.Baggage;
import com.aerolink.baggageservice.service.BaggageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/baggage")
public class BaggageController {

    private final BaggageService baggageService;

    public BaggageController(BaggageService baggageService) {
        this.baggageService = baggageService;
    }

    @PostMapping
    public ResponseEntity<Baggage> createBaggage(
            @RequestBody CreateBaggageRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        /*
         * Only STAFF can reach this endpoint through SecurityConfig.
         * The verified staff access token is forwarded to Booking Service
         * so the confirmed and paid booking can be checked securely.
         */
        Baggage createdBaggage = baggageService.createBaggage(
                request,
                jwt.getTokenValue()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(createdBaggage);
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<Baggage>> getBaggageByBookingId(
            @PathVariable String bookingId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        /*
        * First validate booking access through Booking Service.
        * The verified token is forwarded so:
        * - STAFF may access any valid booking.
        * - PASSENGER may access only their own booking.
        */
        List<Baggage> baggageList =
                baggageService.getBaggageByBookingId(
                        bookingId,
                        jwt.getTokenValue()
                );

        if (isStaff(jwt)) {
            return ResponseEntity.ok(baggageList);
        }

        /*
        * Defence-in-depth:
        * Even after the passenger's booking access is validated,
        * do not expose legacy baggage items that do not contain a trusted userId.
        */
        boolean ownsAllBaggage = baggageList.stream()
                .allMatch(baggage ->
                        baggage.getUserId() != null
                                && jwt.getSubject().equals(baggage.getUserId())
                );

        if (ownsAllBaggage) {
            return ResponseEntity.ok(baggageList);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @GetMapping("/{baggageId}")
    public ResponseEntity<Baggage> getBaggageById(
            @PathVariable String baggageId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return baggageService.getBaggageById(baggageId)
                .map(baggage -> {
                    boolean isStaff = isStaff(jwt);

                    boolean isBaggageOwner = baggage.getUserId() != null
                            && jwt.getSubject().equals(baggage.getUserId());

                    if (isStaff || isBaggageOwner) {
                        return ResponseEntity.ok(baggage);
                    }

                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .<Baggage>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{baggageId}/status")
    public ResponseEntity<Baggage> updateBaggageStatus(
            @PathVariable String baggageId,
            @RequestBody UpdateBaggageStatusRequest request
    ) {
        Baggage updatedBaggage =
                baggageService.updateBaggageStatus(baggageId, request);

        return ResponseEntity.ok(updatedBaggage);
    }

    private boolean isStaff(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");

        return groups != null && groups.contains("STAFF");
    }
}
