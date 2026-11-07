package com.flashCab.driverService.service;

import com.flashCab.driverService.client.LocationServiceClient;
import com.flashCab.driverService.dto.DriverRequest;
import com.flashCab.driverService.dto.DriverResponse;
import com.flashCab.driverService.model.Driver;
import com.flashCab.driverService.repository.DriverRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverRepository driverRepository;
    private final LocationServiceClient locationServiceClient;

    public DriverResponse registerDriver(DriverRequest request) {
        Driver driver = new Driver();
        applyRequest(driver, request);
        driver.setCreatedAt(LocalDateTime.now());
        return mapToResponse(driverRepository.save(driver));
    }

    public List<DriverResponse> getAllDrivers() {
        return driverRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DriverResponse> getDriversByIds(List<String> driverIds) {
        if (driverIds == null || driverIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> validIds = driverIds.stream().filter(this::isValidDriverId).distinct().collect(Collectors.toList());
        if (validIds.isEmpty()) {
            return new ArrayList<>();
        }
        return driverRepository.findAllById(validIds).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public DriverResponse getDriverById(String driverId) {
        return mapToResponse(findDriverById(driverId));
    }

    public DriverResponse updateDriver(String driverId, DriverRequest request) {
        Driver driver = findDriverById(driverId);
        applyRequest(driver, request);
        return mapToResponse(driverRepository.save(driver));
    }

    public void deleteDriver(String driverId) {
        Driver driver = findDriverById(driverId);
        driverRepository.delete(driver);
        try {
            locationServiceClient.removeDriverLocation(driverId);
            log.info("Removed Redis location for deleted driver {}", driverId);
        } catch (FeignException e) {
            log.error("Unable to remove Redis location for deleted driver {}: {}", driverId, e.getMessage());
        }
    }

    private Driver findDriverById(String driverId) {
        return driverRepository.findById(getValidDriverId(driverId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
    }

    private void applyRequest(Driver driver, DriverRequest request) {
        driver.setName(request.getName());
        driver.setEmail(request.getEmail());
        driver.setPhone(request.getPhone());
        driver.setVehicleType(request.getVehicleType());
        driver.setVehicleNumber(request.getVehicleNumber());
    }

    private DriverResponse mapToResponse(Driver driver) {
        return new DriverResponse(
                driver.getId(),
                driver.getName(),
                driver.getEmail(),
                driver.getPhone(),
                driver.getVehicleType(),
                driver.getVehicleNumber(),
                driver.getCreatedAt()
        );
    }

    private boolean isValidDriverId(String driverId) {
        return driverId != null && !driverId.isBlank() && ObjectId.isValid(driverId);
    }

    private String getValidDriverId(String driverId) {
        if (!isValidDriverId(driverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver ID must be a valid MongoDB ObjectId");
        }
        return driverId;
    }
}
