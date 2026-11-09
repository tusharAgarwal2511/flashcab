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

        List<RankedCandidate> rankedCandidates = rankDrivers(nearByDrivers, filteredDrivers);
        if (rankedCandidates.isEmpty()) {
            log.warn("Could not find suitable compatible driver for ride {}", event.getRideId());
            return;
        }

        NearByDriverResponse assignedDriver = null;
        DriverProfileResponse matchedProfile = null;
        for (RankedCandidate candidate : rankedCandidates) {
            boolean reserved;
            try {
                reserved = locationServiceClient.reserveDriver(candidate.driver().getDriverId(), event.getRideId());
            } catch (FeignException e) {
                log.error("Location Service unavailable while reserving a driver for ride {}: {}",
                        event.getRideId(), e.getMessage());
                return;
            }
            if (reserved) {
                assignedDriver = candidate.driver();
                matchedProfile = candidate.profile();
                break;
            }
        }

        if (assignedDriver == null || matchedProfile == null) {
            log.warn("No compatible driver could be reserved for ride {}", event.getRideId());
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

    private List<RankedCandidate> rankDrivers(List<NearByDriverResponse> drivers,
                                               List<DriverProfileResponse> compatibleProfiles) {
        double distanceWeight = 0.7;
        double ratingWeight = 0.3;
        return drivers.stream()
                .flatMap(driver -> compatibleProfiles.stream()
                        .filter(profile -> profile.getId() != null
                                && profile.getId().equals(driver.getDriverId()))
                        .findFirst()
                        .stream()
                        .map(profile -> new RankedCandidate(
                                driver, profile, score(driver, distanceWeight, ratingWeight))))
                .sorted(Comparator.comparingDouble(RankedCandidate::score).reversed())
                .toList();
    }

    private double score(NearByDriverResponse driver, double distanceWeight, double ratingWeight) {
        double distanceScore = 1.0 / (driver.getDistanceInKm() + 0.1);
        double simulatedRating = 4.0 + Math.random();
        return (distanceScore * distanceWeight) + (simulatedRating * ratingWeight);
    }

    private record RankedCandidate(NearByDriverResponse driver, DriverProfileResponse profile, double score) {
    }

}
