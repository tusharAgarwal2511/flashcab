package com.flashCab.riderService.controller;

import com.flashCab.riderService.dto.RiderRequest;
import com.flashCab.riderService.dto.RiderResponse;
import com.flashCab.riderService.service.RiderService;
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
@RequestMapping("/api/v1/riders")
@RequiredArgsConstructor
@Tag(name = "Riders", description = "Rider registration and profile management")
public class RiderController {

    private static final String AUTH_USER_ID_HEADER = "X-Authenticated-User-Id";
    private final RiderService riderService;

    @PostMapping
    @Operation(
            summary = "Register a rider",
            description = "Creates a rider profile using the supplied contact details.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Rider profile details", required = true,
                    content = @Content(schema = @Schema(implementation = RiderRequest.class))))
    @ApiResponses(@ApiResponse(responseCode = "201", description = "Rider registered",
            content = @Content(schema = @Schema(implementation = RiderResponse.class))))
    public ResponseEntity<RiderResponse> registerRider(
            @RequestHeader(AUTH_USER_ID_HEADER) String authUserId,
            @Valid @RequestBody RiderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(riderService.registerRider(request, authUserId));
    }

    @GetMapping
    @Operation(summary = "List riders", description = "Returns all registered riders.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Rider list",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RiderResponse.class)))))
    public ResponseEntity<List<RiderResponse>> getAllRiders(
            @RequestHeader(AUTH_USER_ID_HEADER) String authUserId) {
        return ResponseEntity.ok(riderService.getRidersForUser(authUserId));
    }

    @GetMapping("/{riderId}")
    @Operation(summary = "Get a rider", description = "Returns a rider by identifier.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Rider profile",
            content = @Content(schema = @Schema(implementation = RiderResponse.class))))
    public ResponseEntity<RiderResponse> getRiderById(
            @RequestHeader(AUTH_USER_ID_HEADER) String authUserId,
            @Parameter(description = "Identifier of the rider", required = true) @PathVariable String riderId) {
        return ResponseEntity.ok(riderService.getRiderById(riderId, authUserId));
    }

    @PutMapping("/{riderId}")
    @Operation(
            summary = "Update a rider",
            description = "Replaces the rider profile details for the specified identifier.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated rider profile details", required = true,
                    content = @Content(schema = @Schema(implementation = RiderRequest.class))))
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Updated rider profile",
            content = @Content(schema = @Schema(implementation = RiderResponse.class))))
    public ResponseEntity<RiderResponse> updateRider(
            @RequestHeader(AUTH_USER_ID_HEADER) String authUserId,
            @Parameter(description = "Identifier of the rider", required = true) @PathVariable String riderId,
            @Valid @RequestBody RiderRequest request) {
        return ResponseEntity.ok(riderService.updateRider(riderId, authUserId, request));
    }

    @DeleteMapping("/{riderId}")
    @Operation(summary = "Delete a rider", description = "Deletes the rider with the specified identifier.")
    @ApiResponses(@ApiResponse(responseCode = "204", description = "Rider deleted"))
    public ResponseEntity<Void> deleteRider(
            @RequestHeader(AUTH_USER_ID_HEADER) String authUserId,
            @Parameter(description = "Identifier of the rider", required = true) @PathVariable String riderId) {
        riderService.deleteRider(riderId, authUserId);
        return ResponseEntity.noContent().build();
    }
}
