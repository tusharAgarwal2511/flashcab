package com.flashCab.driverService.controller;

import com.flashCab.driverService.dto.DriverRequest;
import com.flashCab.driverService.dto.DriverResponse;
import com.flashCab.driverService.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PostMapping
    public ResponseEntity<DriverResponse> registerDriver(@Valid @RequestBody DriverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driverService.registerDriver(request));
    }

    @GetMapping
    public ResponseEntity<List<DriverResponse>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    @GetMapping("/batch")
    public ResponseEntity<List<DriverResponse>> getDriversByIds(@RequestParam("ids") List<String> ids) {
        return ResponseEntity.ok(driverService.getDriversByIds(ids));
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<DriverResponse> getDriverById(@PathVariable String driverId) {
        return ResponseEntity.ok(driverService.getDriverById(driverId));
    }

    @PutMapping("/{driverId}")
    public ResponseEntity<DriverResponse> updateDriver(
            @PathVariable String driverId,
            @Valid @RequestBody DriverRequest request) {
        return ResponseEntity.ok(driverService.updateDriver(driverId, request));
    }

    @DeleteMapping("/{driverId}")
    public ResponseEntity<Void> deleteDriver(@PathVariable String driverId) {
        driverService.deleteDriver(driverId);
        return ResponseEntity.noContent().build();
    }
} 
