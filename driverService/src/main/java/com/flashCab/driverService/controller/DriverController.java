package com.flashCab.driverService.controller;

import com.flashCab.driverService.dto.DriverRequest;
import com.flashCab.driverService.dto.DriverResponse;
import com.flashCab.driverService.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
@Tag(name = "Drivers", description = "Driver profile management")
public class DriverController {

    private static final String AUTH_USER_ID_HEADER = "X-Authenticated-User-Id";
    private final DriverService driverService;

    @GetMapping("/{driverId}")
    @Operation(summary = "Get a driver", description = "Returns a driver by identifier.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Driver profile",
            content = @Content(schema = @Schema(implementation = DriverResponse.class))))
    public ResponseEntity<DriverResponse> getDriverById(
            @RequestHeader(AUTH_USER_ID_HEADER) String authUserId,
            @Parameter(description = "Identifier of the driver", required = true) @PathVariable String driverId) {
        return ResponseEntity.ok(driverService.getDriverById(driverId, authUserId));
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
            @RequestHeader(AUTH_USER_ID_HEADER) String authUserId,
            @Parameter(description = "Identifier of the driver", required = true) @PathVariable String driverId,
            @Valid @RequestBody DriverRequest request) {
        return ResponseEntity.ok(driverService.updateDriver(driverId, authUserId, request));
    }

    @DeleteMapping("/{driverId}")
    @Operation(summary = "Delete a driver", description = "Deletes the driver with the specified identifier.")
    @ApiResponses(@ApiResponse(responseCode = "204", description = "Driver deleted"))
    public ResponseEntity<Void> deleteDriver(
            @RequestHeader(AUTH_USER_ID_HEADER) String authUserId,
            @Parameter(description = "Identifier of the driver", required = true) @PathVariable String driverId) {
        driverService.deleteDriver(driverId, authUserId);
        return ResponseEntity.noContent().build();
    }
} 
