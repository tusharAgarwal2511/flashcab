package com.flashCab.driverService.service;

import com.flashCab.driverService.client.LocationServiceClient;
import com.flashCab.driverService.dto.DriverProfileProvisionRequest;
import com.flashCab.driverService.dto.DriverRequest;
import com.flashCab.driverService.dto.DriverResponse;
import com.flashCab.driverService.model.Driver;
import com.flashCab.driverService.repository.DriverRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
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

    public DriverResponse registerDriver(DriverRequest request, String authUserId) {
        requireAuthUserId(authUserId);
        if (driverRepository.existsByAuthUserId(authUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Driver profile already exists");
        }
        Driver driver = new Driver();
        applyRequest(driver, request);
        driver.setAuthUserId(authUserId);
        driver.setCreatedAt(LocalDateTime.now());
        try {
            return mapToResponse(driverRepository.save(driver));
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Driver profile already exists");
        }
    }

    public DriverResponse provisionDriver(DriverProfileProvisionRequest request) {
        requireAuthUserId(request.getAuthUserId());
        return driverRepository.findByAuthUserId(request.getAuthUserId())
                .map(this::mapToResponse)
                .orElseGet(() -> {
                    Driver driver = new Driver();
                    driver.setAuthUserId(request.getAuthUserId());
                    driver.setName(request.getName());
                    driver.setEmail(request.getEmail());
                    driver.setPhone(request.getPhone());
                    driver.setVehicleType(request.getVehicleType());
                    driver.setVehicleNumber(request.getVehicleNumber());
                    driver.setCreatedAt(LocalDateTime.now());
                    try {
                        return mapToResponse(driverRepository.save(driver));
                    } catch (DuplicateKeyException exception) {
                        return driverRepository.findByAuthUserId(request.getAuthUserId())
                                .map(this::mapToResponse)
                                .orElseThrow(() -> exception);
                    }
                });
    }

    public List<DriverResponse> getDriversForUser(String authUserId) {
        return List.of(getDriverByAuthUserId(authUserId));
    }

    public List<DriverResponse> getInternalDrivers() {
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

    public DriverResponse getDriverById(String driverId, String authUserId) {
        Driver driver = findDriverById(driverId);
        requireOwner(driver, authUserId);
        return mapToResponse(driver);
    }

    public DriverResponse getInternalDriverById(String driverId) {
        return mapToResponse(findDriverById(driverId));
    }

    public DriverResponse getDriverByAuthUserId(String authUserId) {
        requireAuthUserId(authUserId);
        return driverRepository.findByAuthUserId(authUserId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found"));
    }

    public DriverResponse updateDriver(String driverId, String authUserId, DriverRequest request) {
        Driver driver = findOwnedDriverById(driverId, authUserId);
        applyRequest(driver, request);
        return mapToResponse(driverRepository.save(driver));
    }

    public void deleteDriver(String driverId, String authUserId) {
        Driver driver = findOwnedDriverById(driverId, authUserId);
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

    private Driver findOwnedDriverById(String driverId, String authUserId) {
        Driver driver = findDriverById(driverId);
        requireOwner(driver, authUserId);
        return driver;
    }

    private void requireOwner(Driver driver, String authUserId) {
        requireAuthUserId(authUserId);
        if (!authUserId.equals(driver.getAuthUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver profile does not belong to this account");
        }
    }

    private void requireAuthUserId(String authUserId) {
        if (authUserId == null || authUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }
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
                driver.getAuthUserId(),
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
