package com.flashCab.rideService.controller;


import com.flashCab.rideService.dto.RideRequest;
import com.flashCab.rideService.dto.RideResponse;
import com.flashCab.rideService.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rides")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Rides", description = "Ride booking and lifecycle operations")
public class RideController {
    private static final String AUTH_USER_ID_HEADER = "X-Authenticated-User-Id";
    private static final String AUTH_USER_ROLE_HEADER = "X-Authenticated-User-Role";
    private final RideService rideService;

    @PostMapping("/request")
    @Operation(
            summary = "Request a ride",
            description = "Creates a ride request with pickup, drop-off, and vehicle details.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Ride request details", required = true,
                    content = @Content(schema = @Schema(implementation = RideRequest.class))))
    @ApiResponses(@ApiResponse(responseCode = "201", description = "Ride request created",
            content = @Content(schema = @Schema(implementation = RideResponse.class))))
    public ResponseEntity<RideResponse> requestRide(
            @RequestHeader(AUTH_USER_ID_HEADER) String authUserId,
            @Valid @RequestBody RideRequest request) {
        log.info("Ride request received from authenticated user: {}", authUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(rideService.requestRide(request, authUserId));
    }

    @GetMapping("/{rideId}")
    @Operation(summary = "Get a ride", description = "Returns a ride by identifier.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Ride details",
            content = @Content(schema = @Schema(implementation = RideResponse.class))))
    public ResponseEntity<RideResponse> getRideById(
            @RequestHeader(AUTH_USER_ID_HEADER) String authUserId,
            @RequestHeader(AUTH_USER_ROLE_HEADER) String role,
            @Parameter(description = "Identifier of the ride", required = true) @PathVariable String rideId) {
        return ResponseEntity.ok(rideService.getRideById(rideId, authUserId, role));
    }

    @GetMapping("/rider/{riderId}")
    @Operation(summary = "List rides for a rider", description = "Returns the rides associated with the specified rider.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Rides for the rider",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RideResponse.class)))))
    public ResponseEntity<List<RideResponse>> getRidesByRider(
            @RequestHeader(AUTH_USER_ID_HEADER) String authUserId,
            @Parameter(description = "Identifier of the rider", required = true) @PathVariable String riderId) {
        return ResponseEntity.ok(rideService.getRidesByRider(riderId, authUserId));
    }

    @PutMapping("/{rideId}/start")
    @Operation(summary = "Start a ride", description = "Marks the specified ride as started.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Updated ride details",
            content = @Content(schema = @Schema(implementation = RideResponse.class))))
    public ResponseEntity<RideResponse> startRide(
            @RequestHeader(AUTH_USER_ID_HEADER) String authUserId,
            @Parameter(description = "Identifier of the ride", required = true) @PathVariable String rideId) {
        return ResponseEntity.ok(rideService.startRide(rideId, authUserId));
    }

    @PutMapping("/{rideId}/complete")
    @Operation(summary = "Complete a ride", description = "Marks the specified ride as completed.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Updated ride details",
            content = @Content(schema = @Schema(implementation = RideResponse.class))))
    public ResponseEntity<RideResponse> completeRide(
            @RequestHeader(AUTH_USER_ID_HEADER) String authUserId,
            @Parameter(description = "Identifier of the ride", required = true) @PathVariable String rideId) {
        return ResponseEntity.ok(rideService.completeRide(rideId, authUserId));
    }

    @PutMapping("/{rideId}/cancel")
    @Operation(summary = "Cancel a ride", description = "Cancels the specified ride.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Updated ride details",
            content = @Content(schema = @Schema(implementation = RideResponse.class))))
    public ResponseEntity<RideResponse> cancelRide(
            @RequestHeader(AUTH_USER_ID_HEADER) String authUserId,
            @Parameter(description = "Identifier of the ride", required = true) @PathVariable String rideId) {
        return ResponseEntity.ok(rideService.cancelRide(rideId, authUserId));
    }
}
