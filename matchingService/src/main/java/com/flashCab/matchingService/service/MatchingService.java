package com.flashCab.matchingService.service;


import com.flashCab.matchingService.client.LocationServiceClient;
import com.flashCab.matchingService.dto.NearByDriverResponse;
import com.flashCab.matchingService.event.RideMatchedEvent;
import com.flashCab.matchingService.event.RideRequestedEvent;
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
    private final KafkaTemplate<String, RideMatchedEvent> kafkaTemplate;

    public void matchDriverForRide(RideRequestedEvent event) {

        List<NearByDriverResponse> nearByDrivers = locationServiceClient.getNearByDrivers(
                event.getPickupLatitude(),
                event.getPickupLongitude(),
                DEFAULT_SEARCH_RADIUS_KM
        );

        if (nearByDrivers.isEmpty()) {
            log.warn("Not drivers found near ride");
            return;
        }

        Optional<NearByDriverResponse> bestDriver = findBestDriver(nearByDrivers);
        if (bestDriver.isEmpty()) {
            log.warn("Could not find suitable driver for ride");
            return;
        }

        NearByDriverResponse assignedDriver = bestDriver.get();
        RideMatchedEvent matchedEvent = new RideMatchedEvent(
                event.getRideId(),
                event.getRiderId(),
                assignedDriver.getDriverId(),
                assignedDriver.getLatitude(),
                assignedDriver.getLongitude(),
                assignedDriver.getDistanceInKm()
        );
        kafkaTemplate.send(RIDE_MATCHED_TOPIC, event.getRideId(), matchedEvent);
        log.info("RideMatchedEvent published");
    }

    private Optional<NearByDriverResponse> findBestDriver(List<NearByDriverResponse> drivers) {
        double distanceWeight = 0.7;
        double ratingWeight = 0.3;
        return drivers.stream().max(Comparator.comparingDouble(driver -> {
            double distanceScore = 1.0 / (driver.getDistanceInKm() + 0.1);
            // Simulated Rating between 4.0 and 5.0
            // in prod fetch from driver service
            double simulatedRating = 4.0 + Math.random();
            return (distanceScore * distanceWeight) + (simulatedRating * ratingWeight);
        }));
    }

}
