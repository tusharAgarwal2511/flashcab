package com.flashCab.locationService.controller;

import com.flashCab.locationService.dto.DriverLocationRequest;
import com.flashCab.locationService.dto.NearByDriverResponse;
import com.flashCab.locationService.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Driver location and availability operations")
public class LocationController {
    private final LocationService locationService;

    @PostMapping("/drivers/update")
    @Operation(
            summary = "Update a driver's location",
            description = "Stores the driver's latest coordinates.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Driver identifier and coordinates", required = true,
                    content = @Content(schema = @Schema(implementation = DriverLocationRequest.class))))
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Location updated",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))))
    public ResponseEntity<String> updateDriverLocation(@RequestBody DriverLocationRequest driverLocationRequest) {
        locationService.updateDriverLocation(driverLocationRequest);
        return ResponseEntity.ok("Driver location updated");
    }

    @GetMapping("/drivers/seed")
    @Operation(summary = "Seed driver locations", description = "Adds driver locations within the specified coordinate bounds.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Number of locations added",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))))
    public ResponseEntity<String> seedDrivers(
            @Parameter(description = "Number of driver locations to add", required = true) @RequestParam int count,
            @Parameter(description = "Minimum latitude of the seed area", required = true)
            @RequestParam double minLatitude,
            @Parameter(description = "Maximum latitude of the seed area", required = true)
            @RequestParam double maxLatitude,
            @Parameter(description = "Minimum longitude of the seed area", required = true)
            @RequestParam double minLongitude,
            @Parameter(description = "Maximum longitude of the seed area", required = true)
            @RequestParam double maxLongitude) {
        int added = locationService.seedDrivers(count, minLatitude, maxLatitude, minLongitude, maxLongitude);
        return ResponseEntity.ok(added + " drivers added to Redis");
    }

    @GetMapping("/drivers/nearby")
    @Operation(summary = "Find nearby drivers", description = "Returns drivers within the requested radius of the supplied coordinates.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Nearby drivers",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = NearByDriverResponse.class)))))
    public ResponseEntity<List<NearByDriverResponse>> getNearByDrivers(
            @Parameter(description = "Search center latitude", required = true) @RequestParam double latitude,
            @Parameter(description = "Search center longitude", required = true) @RequestParam double longitude,
            @Parameter(description = "Search radius in kilometers; defaults to 5.0")
            @RequestParam(defaultValue = "5.0") double radiusInKm) {
        return ResponseEntity.ok(locationService.findNearByDrivers(latitude, longitude, radiusInKm));
    }

    @PostMapping("/drivers/{driverId}/reserve")
    @Operation(summary = "Reserve a driver", description = "Reserves the specified driver for a ride.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Whether the driver was reserved",
            content = @Content(schema = @Schema(implementation = Boolean.class))))
    public ResponseEntity<Boolean> reserveDriver(
            @Parameter(description = "Identifier of the driver", required = true) @PathVariable String driverId,
            @Parameter(description = "Identifier of the ride", required = true) @RequestParam String rideId) {
        return ResponseEntity.ok(locationService.reserveDriver(driverId, rideId));
    }

    @PostMapping("/drivers/{driverId}/release")
    @Operation(summary = "Release a driver", description = "Releases the specified driver's reservation for a ride.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Whether the driver was released",
            content = @Content(schema = @Schema(implementation = Boolean.class))))
    public ResponseEntity<Boolean> releaseDriver(
            @Parameter(description = "Identifier of the driver", required = true) @PathVariable String driverId,
            @Parameter(description = "Identifier of the ride", required = true) @RequestParam String rideId) {
        return ResponseEntity.ok(locationService.releaseDriver(driverId, rideId));
    }

    @DeleteMapping("/drivers/{driverId}")
    @Operation(summary = "Remove a driver location", description = "Removes the specified driver's location and availability entry.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Driver location removed",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))))
    public ResponseEntity<String> removeDriver(
            @Parameter(description = "Identifier of the driver", required = true) @PathVariable String driverId) {
        locationService.removeDriver(driverId);
        return ResponseEntity.ok("Driver Removed");

    }
}
