package com.aerolink.baggageservice.controller;

import com.aerolink.baggageservice.dto.CreateBaggageRequest;
import com.aerolink.baggageservice.model.Baggage;
import com.aerolink.baggageservice.service.BaggageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/baggage")
public class BaggageController {

    private final BaggageService baggageService;

    public BaggageController(BaggageService baggageService) {
        this.baggageService = baggageService;
    }

    @PostMapping
    public ResponseEntity<Baggage> createBaggage(
            @RequestBody CreateBaggageRequest request
    ) {
        Baggage createdBaggage = baggageService.createBaggage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBaggage);
    }

    @GetMapping("/{baggageId}")
    public ResponseEntity<Baggage> getBaggageById(
            @PathVariable String baggageId
    ) {
        return baggageService.getBaggageById(baggageId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}