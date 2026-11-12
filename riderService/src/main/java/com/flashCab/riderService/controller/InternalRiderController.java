package com.flashCab.riderService.controller;

import com.flashCab.riderService.dto.RiderProfileProvisionRequest;
import com.flashCab.riderService.dto.RiderResponse;
import com.flashCab.riderService.service.RiderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/riders")
@RequiredArgsConstructor
public class InternalRiderController {

    private final RiderService riderService;

    @PostMapping
    public ResponseEntity<RiderResponse> provisionRider(
            @Valid @RequestBody RiderProfileProvisionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(riderService.provisionRider(request));
    }

    @GetMapping("/{riderId}")
    public ResponseEntity<RiderResponse> getRiderById(@PathVariable String riderId) {
        return ResponseEntity.ok(riderService.getInternalRiderById(riderId));
    }

    @GetMapping("/by-auth-user/{authUserId}")
    public ResponseEntity<RiderResponse> getRiderByAuthUserId(@PathVariable String authUserId) {
        return ResponseEntity.ok(riderService.getRiderByAuthUserId(authUserId));
    }
}
