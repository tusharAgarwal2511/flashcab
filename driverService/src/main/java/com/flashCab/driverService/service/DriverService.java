package com.flashCab.driverService.service;

import com.flashCab.driverService.dto.DriverRequest;
import com.flashCab.driverService.dto.DriverResponse;
import com.flashCab.driverService.model.Driver;
import com.flashCab.driverService.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;

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

    public DriverResponse getDriverById(String driverId) {
        return mapToResponse(findDriverById(driverId));
    }

    public DriverResponse updateDriver(String driverId, DriverRequest request) {
        Driver driver = findDriverById(driverId);
        applyRequest(driver, request);
        return mapToResponse(driverRepository.save(driver));
    }

    public void deleteDriver(String driverId) {
        driverRepository.delete(findDriverById(driverId));
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

    private String getValidDriverId(String driverId) {
        if (!ObjectId.isValid(driverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver ID must be a valid MongoDB ObjectId");
        }
        return driverId;
    }
}
