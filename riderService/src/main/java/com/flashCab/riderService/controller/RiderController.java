package com.flashCab.riderService.controller;

import com.flashCab.riderService.dto.RiderRequest;
import com.flashCab.riderService.dto.RiderResponse;
import com.flashCab.riderService.service.RiderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/riders")
@RequiredArgsConstructor
public class RiderController {

    private final RiderService riderService;

    @PostMapping
    public ResponseEntity<RiderResponse> registerRider(@Valid @RequestBody RiderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(riderService.registerRider(request));
    }

    @GetMapping
    public ResponseEntity<List<RiderResponse>> getAllRiders() {
        return ResponseEntity.ok(riderService.getAllRiders());
    }

    @GetMapping("/{riderId}")
    public ResponseEntity<RiderResponse> getRiderById(@PathVariable String riderId) {
        return ResponseEntity.ok(riderService.getRiderById(riderId));
    }

    @PutMapping("/{riderId}")
    public ResponseEntity<RiderResponse> updateRider(
            @PathVariable String riderId,
            @Valid @RequestBody RiderRequest request) {
        return ResponseEntity.ok(riderService.updateRider(riderId, request));
    }

    @DeleteMapping("/{riderId}")
    public ResponseEntity<Void> deleteRider(@PathVariable String riderId) {
        riderService.deleteRider(riderId);
        return ResponseEntity.noContent().build();
    }
}
