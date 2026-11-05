package com.flashCab.locationService.controller;

import com.flashCab.locationService.dto.DriverLocationRequest;
import com.flashCab.locationService.dto.NearByDriverResponse;
import com.flashCab.locationService.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@Slf4j
@RequiredArgsConstructor
public class LocationController {
    private final LocationService locationService;

    @PostMapping("/drivers/update")
    public ResponseEntity<String> updateDriverLocation(@RequestBody DriverLocationRequest driverLocationRequest) {
        locationService.updateDriverLocation(driverLocationRequest);
        return ResponseEntity.ok("Driver location updated");
    }

    @GetMapping("/drivers/nearby")
    public ResponseEntity<List<NearByDriverResponse>> getNearByDrivers(@RequestParam double latitude, @RequestParam double longitude, @RequestParam(defaultValue = "5.0") double radiusInKm) {
        return ResponseEntity.ok(locationService.findNearByDrivers(latitude, longitude, radiusInKm));
    }

    @DeleteMapping("/drivers/{driverId}")
    public ResponseEntity<String> removeDriver(@PathVariable String driverId) {
        locationService.removeDriver(driverId);
        return ResponseEntity.ok("Driver Removed");

    }
}
