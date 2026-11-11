package com.flashCab.driverService.controller;

import com.flashCab.driverService.dto.DriverRequest;
import com.flashCab.driverService.dto.DriverResponse;
import com.flashCab.driverService.service.DriverService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
@Tag(name = "Drivers", description = "Driver registration and profile management")
public class DriverController {

    private final DriverService driverService;

    @PostMapping
    @Operation(
            summary = "Register a driver",
            description = "Creates a driver profile with contact and vehicle details.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Driver profile and vehicle details", required = true,
                    content = @Content(schema = @Schema(implementation = DriverRequest.class))))
    @ApiResponses(@ApiResponse(responseCode = "201", description = "Driver registered",
            content = @Content(schema = @Schema(implementation = DriverResponse.class))))
    public ResponseEntity<DriverResponse> registerDriver(@Valid @RequestBody DriverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driverService.registerDriver(request));
    }

    @GetMapping
    @Operation(summary = "List drivers", description = "Returns all registered drivers.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Driver list",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = DriverResponse.class)))))
    public ResponseEntity<List<DriverResponse>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    @GetMapping("/batch")
    @Operation(summary = "Get drivers by identifiers", description = "Returns drivers matching the supplied identifier list.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Matching drivers",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = DriverResponse.class)))))
    public ResponseEntity<List<DriverResponse>> getDriversByIds(
            @Parameter(description = "Driver identifiers to retrieve", required = true)
            @RequestParam("ids") List<String> ids) {
        return ResponseEntity.ok(driverService.getDriversByIds(ids));
    }

    @GetMapping("/{driverId}")
    @Operation(summary = "Get a driver", description = "Returns a driver by identifier.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Driver profile",
            content = @Content(schema = @Schema(implementation = DriverResponse.class))))
    public ResponseEntity<DriverResponse> getDriverById(
            @Parameter(description = "Identifier of the driver", required = true) @PathVariable String driverId) {
        return ResponseEntity.ok(driverService.getDriverById(driverId));
    }

    @PutMapping("/{driverId}")
    @Operation(
            summary = "Update a driver",
            description = "Replaces the driver profile details for the specified identifier.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated driver profile and vehicle details", required = true,
                    content = @Content(schema = @Schema(implementation = DriverRequest.class))))
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Updated driver profile",
            content = @Content(schema = @Schema(implementation = DriverResponse.class))))
    public ResponseEntity<DriverResponse> updateDriver(
            @Parameter(description = "Identifier of the driver", required = true) @PathVariable String driverId,
            @Valid @RequestBody DriverRequest request) {
        return ResponseEntity.ok(driverService.updateDriver(driverId, request));
    }

    @DeleteMapping("/{driverId}")
    @Operation(summary = "Delete a driver", description = "Deletes the driver with the specified identifier.")
    @ApiResponses(@ApiResponse(responseCode = "204", description = "Driver deleted"))
    public ResponseEntity<Void> deleteDriver(
            @Parameter(description = "Identifier of the driver", required = true) @PathVariable String driverId) {
        driverService.deleteDriver(driverId);
        return ResponseEntity.noContent().build();
    }
} 
