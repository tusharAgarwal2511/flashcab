package com.flashCab.driverService.controller;

import com.flashCab.driverService.dto.DriverProfileProvisionRequest;
import com.flashCab.driverService.dto.DriverResponse;
import com.flashCab.driverService.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/drivers")
@RequiredArgsConstructor
public class InternalDriverController {

    private final DriverService driverService;

    @PostMapping
    public ResponseEntity<DriverResponse> provisionDriver(
            @Valid @RequestBody DriverProfileProvisionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driverService.provisionDriver(request));
    }

    @GetMapping
    public ResponseEntity<List<DriverResponse>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getInternalDrivers());
    }

    @GetMapping("/batch")
    public ResponseEntity<List<DriverResponse>> getDriversByIds(@RequestParam("ids") List<String> ids) {
        return ResponseEntity.ok(driverService.getDriversByIds(ids));
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<DriverResponse> getDriverById(@PathVariable String driverId) {
        return ResponseEntity.ok(driverService.getInternalDriverById(driverId));
    }

    @GetMapping("/by-auth-user/{authUserId}")
    public ResponseEntity<DriverResponse> getDriverByAuthUserId(@PathVariable String authUserId) {
        return ResponseEntity.ok(driverService.getDriverByAuthUserId(authUserId));
    }
}
