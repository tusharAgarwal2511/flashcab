package com.flashCab.matchingService.service;


import com.flashCab.matchingService.client.DriverServiceClient;
import com.flashCab.matchingService.client.LocationServiceClient;
import com.flashCab.matchingService.dto.DriverProfileResponse;
import com.flashCab.matchingService.dto.NearByDriverResponse;
import com.flashCab.matchingService.event.RideMatchedEvent;
import com.flashCab.matchingService.event.RideRequestedEvent;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchingService {

    private static final String RIDE_MATCHED_TOPIC = "ride.matched";
    private static final double DEFAULT_SEARCH_RADIUS_KM = 100.0;
    private final LocationServiceClient locationServiceClient;
    private final DriverServiceClient driverServiceClient;
    private final KafkaTemplate<String, RideMatchedEvent> kafkaTemplate;

    public void matchDriverForRide(RideRequestedEvent event) {
        if (event == null || event.getRideId() == null || event.getPickupLatitude() == null || event.getPickupLongitude() == null) {
            log.warn("Invalid ride request received for matching");
            return;
        }

        List<NearByDriverResponse> nearByDrivers;
        try {
            nearByDrivers = locationServiceClient.getNearByDrivers(
                    event.getPickupLatitude(),
                    event.getPickupLongitude(),
                    DEFAULT_SEARCH_RADIUS_KM
            );
        } catch (FeignException e) {
            log.error("Location Service unavailable while matching ride {}: {}", event.getRideId(), e.getMessage());
            return;
        }

        if (nearByDrivers == null || nearByDrivers.isEmpty()) {
            log.warn("No drivers found near ride {}", event.getRideId());
            return;
        }

        List<String> driverIds = nearByDrivers.stream()
                .map(NearByDriverResponse::getDriverId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        List<DriverProfileResponse> compatibleDrivers;
        try {
            compatibleDrivers = driverServiceClient.getDriversByIds(driverIds);
        } catch (FeignException e) {
            log.error("Driver Service unavailable while matching ride {}: {}", event.getRideId(), e.getMessage());
            return;
        }

        if (compatibleDrivers == null || compatibleDrivers.isEmpty()) {
            log.warn("No driver profiles found for nearby drivers for ride {}", event.getRideId());
            return;
        }

        String requestedVehicleType = event.getVehicleType();
        List<DriverProfileResponse> filteredDrivers = compatibleDrivers.stream()
                .filter(driver -> driver.getVehicleType() != null)
                .filter(driver -> driver.getVehicleType().equalsIgnoreCase(requestedVehicleType))
                .toList();

        if (filteredDrivers.isEmpty()) {
            log.warn("No nearby drivers match requested vehicle type {} for ride {}", requestedVehicleType, event.getRideId());
            return;
        }

        Optional<NearByDriverResponse> bestDriver = findBestDriver(nearByDrivers, filteredDrivers);
        if (bestDriver.isEmpty()) {
            log.warn("Could not find suitable compatible driver for ride {}", event.getRideId());
            return;
        }

        NearByDriverResponse assignedDriver = bestDriver.get();
        DriverProfileResponse matchedProfile = filteredDrivers.stream()
                .filter(driver -> driver.getId().equals(assignedDriver.getDriverId()))
                .findFirst()
                .orElse(null);

        if (matchedProfile == null) {
            log.warn("Matched driver profile not found for ride {}", event.getRideId());
            return;
        }

        RideMatchedEvent matchedEvent = new RideMatchedEvent(
                event.getRideId(),
                event.getRiderId(),
                assignedDriver.getDriverId(),
                assignedDriver.getLatitude(),
                assignedDriver.getLongitude(),
                assignedDriver.getDistanceInKm(),
                matchedProfile.getVehicleType()
        );
        kafkaTemplate.send(RIDE_MATCHED_TOPIC, event.getRideId(), matchedEvent);
        log.info("RideMatchedEvent published for ride {} with driver {} and vehicle type {}", event.getRideId(), assignedDriver.getDriverId(), matchedProfile.getVehicleType());
    }

    private Optional<NearByDriverResponse> findBestDriver(List<NearByDriverResponse> drivers, List<DriverProfileResponse> compatibleProfiles) {
        double distanceWeight = 0.7;
        double ratingWeight = 0.3;
        return drivers.stream()
                .filter(driver -> compatibleProfiles.stream().anyMatch(profile -> profile.getId().equals(driver.getDriverId())))
                .max(Comparator.comparingDouble(driver -> {
                    double distanceScore = 1.0 / (driver.getDistanceInKm() + 0.1);
                    double simulatedRating = 4.0 + Math.random();
                    return (distanceScore * distanceWeight) + (simulatedRating * ratingWeight);
                }));
    }

}
