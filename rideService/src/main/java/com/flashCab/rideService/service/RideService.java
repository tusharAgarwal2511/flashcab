package com.flashCab.rideService.service;

import com.flashCab.rideService.dto.RideRequest;
import com.flashCab.rideService.dto.RideResponse;
import com.flashCab.rideService.event.RideRequestedEvent;
import com.flashCab.rideService.model.Ride;
import com.flashCab.rideService.model.RideStatus;
import com.flashCab.rideService.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideService {

    private static final String RIDE_REQUESTED_TOPIC = "ride.requested";
    private final RideRepository rideRepository;
    private final KafkaTemplate<String, RideRequestedEvent> kafkaTemplate;

    public RideResponse requestRide(RideRequest request) {
        log.info("New ride request from rider: {}", request.getRiderId());
        Ride ride = new Ride();
        ride.setRiderId(request.getRiderId());
        ride.setPickupLatitude(request.getPickupLatitude());
        ride.setPickupLongitude(request.getPickupLongitude());
        ride.setPickupAddress(request.getPickupAddress());
        ride.setDropLatitude(request.getDropLatitude());
        ride.setDropLongitude(request.getDropLongitude());
        ride.setDropAddress(request.getDropAddress());
        ride.setStatus(RideStatus.REQUESTED);
        ride.setEstimatedFare(calculateEstimateFare(request));

        Ride savedRide = rideRepository.save(ride);

        RideRequestedEvent event = new RideRequestedEvent(
                savedRide.getId(),
                savedRide.getRiderId(),
                savedRide.getPickupLatitude(),
                savedRide.getPickupLongitude(),
                savedRide.getPickupAddress(),
                savedRide.getDropLatitude(),
                savedRide.getDropLongitude(),
                savedRide.getDropAddress()
        );

        kafkaTemplate.send(RIDE_REQUESTED_TOPIC, savedRide.getId(), event);
        log.info("Ride requested event published to Kafka for ride: {}", savedRide.getId());
        savedRide.setStatus(RideStatus.MATCHING);
        rideRepository.save(savedRide);
        return mapToResponse(savedRide);
    }


    public void updateRideWithDriver(String rideId, String driverId) {

        Ride ride = rideRepository.findById(rideId).orElseThrow(() -> new RuntimeException("Ride not found"));
        ride.setDriverId(driverId);
        ride.setStatus(RideStatus.ACCEPTED);
        rideRepository.save(ride);

    }

    public RideResponse startRide(String rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow(() -> new RuntimeException("Ride not found"));
        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new RuntimeException("Ride cannot be started, Current status: " + ride.getStatus());
        }
        ride.setStatus(RideStatus.RIDE_STARTED);
        ride.setStartedAt(LocalDateTime.now());
        rideRepository.save(ride);
        return mapToResponse(ride);
    }

    public RideResponse completeRide(String rideId) {

        Ride ride = rideRepository.findById(rideId).orElseThrow(() -> new RuntimeException("Ride not found"));
        if (ride.getStatus() != RideStatus.RIDE_STARTED) {
            throw new RuntimeException("Ride cannot be completed, Current status: " + ride.getStatus());
        }
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());
        ride.setActualFare(ride.getEstimatedFare());
        rideRepository.save(ride);
        return mapToResponse(ride);
    }

    public RideResponse cancelRide(String rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow(() -> new RuntimeException("Ride not found"));
        ride.setStatus(RideStatus.CANCELLED);
        rideRepository.save(ride);
        return mapToResponse(ride);
    }

    public RideResponse getRideById(String rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow(() -> new RuntimeException("Ride not found"));
        return mapToResponse(ride);
    }

    public List<RideResponse> getRidesByRider(String riderId) {
        return rideRepository.findByRiderIdOrderByCreatedAtDesc(riderId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

    }


    private Double calculateEstimateFare(RideRequest request) {
        double lat1 = Math.toRadians(request.getPickupLatitude());
        double lat2 = Math.toRadians(request.getDropLatitude());
        double lon1 = Math.toRadians(request.getPickupLongitude());
        double lon2 = Math.toRadians(request.getDropLongitude());
        double latDiff = lat2 - lat1;
        double lonDiff = lon1 - lon2;

        double a = Math.pow(Math.sin(latDiff / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(lonDiff / 2), 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double distanceKm = 6371 * c;
        double fare = 50 + (distanceKm * 12);
        return Math.round(fare * 100.0) / 100.0;
    }

    private RideResponse mapToResponse(Ride savedRide) {
        RideResponse response = new RideResponse();

        response.setId(savedRide.getId());
        response.setRiderId(savedRide.getRiderId());
        response.setDriverId(savedRide.getDriverId());

        response.setPickupLatitude(savedRide.getPickupLatitude());
        response.setPickupLongitude(savedRide.getPickupLongitude());
        response.setPickupAddress(savedRide.getPickupAddress());

        response.setDropLatitude(savedRide.getDropLatitude());
        response.setDropLongitude(savedRide.getDropLongitude());
        response.setDropAddress(savedRide.getDropAddress());

        response.setStatus(savedRide.getStatus());

        response.setEstimatedFare(savedRide.getEstimatedFare());
        response.setActualFare(savedRide.getActualFare());

        response.setCreatedAt(savedRide.getCreatedAt());
        response.setStartedAt(savedRide.getStartedAt());
        response.setCompletedAt(savedRide.getCompletedAt());

        return response;
    }


}
