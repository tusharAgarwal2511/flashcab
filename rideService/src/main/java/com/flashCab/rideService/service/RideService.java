package com.flashCab.rideService.service;

import com.flashCab.rideService.client.DriverServiceClient;
import com.flashCab.rideService.client.OpenMeteoWeatherClient;
import com.flashCab.rideService.client.RiderServiceClient;
import com.flashCab.rideService.dto.OpenMeteoWeatherResponse;
import com.flashCab.rideService.dto.RideRequest;
import com.flashCab.rideService.dto.RideResponse;
import com.flashCab.rideService.event.RideRequestedEvent;
import com.flashCab.rideService.model.Ride;
import com.flashCab.rideService.model.RideStatus;
import com.flashCab.rideService.model.VehicleType;
import com.flashCab.rideService.repository.RideRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideService {

    private static final String RIDE_REQUESTED_TOPIC = "ride.requested";
    private static final ZoneId BOOKING_TIME_ZONE = ZoneId.of("Asia/Kolkata");
    private final RideRepository rideRepository;
    private final KafkaTemplate<String, RideRequestedEvent> kafkaTemplate;
    private final RiderServiceClient riderServiceClient;
    private final DriverServiceClient driverServiceClient;
    private final VehicleFarePolicy vehicleFarePolicy;
    private final OpenMeteoWeatherClient openMeteoWeatherClient;

    public RideResponse requestRide(RideRequest request) {
        log.info("New ride request from rider: {}", request.getRiderId());
        LocalTime bookingTime = LocalTime.now(BOOKING_TIME_ZONE);
        validateRiderExists(request.getRiderId());

        Ride ride = new Ride();
        ride.setRiderId(request.getRiderId());
        ride.setVehicleType(request.getVehicleType());
        ride.setPickupLatitude(request.getPickupLatitude());
        ride.setPickupLongitude(request.getPickupLongitude());
        ride.setPickupAddress(request.getPickupAddress());
        ride.setDropLatitude(request.getDropLatitude());
        ride.setDropLongitude(request.getDropLongitude());
        ride.setDropAddress(request.getDropAddress());
        ride.setStatus(RideStatus.REQUESTED);
        ride.setEstimatedFare(calculateEstimateFare(request, bookingTime));

        Ride savedRide = rideRepository.save(ride);

        RideRequestedEvent event = new RideRequestedEvent(
                savedRide.getId(),
                savedRide.getRiderId(),
                savedRide.getPickupLatitude(),
                savedRide.getPickupLongitude(),
                savedRide.getPickupAddress(),
                savedRide.getDropLatitude(),
                savedRide.getDropLongitude(),
                savedRide.getDropAddress(),
                savedRide.getVehicleType());

        kafkaTemplate.send(RIDE_REQUESTED_TOPIC, savedRide.getId(), event);
        log.info("Ride requested event published to Kafka for ride: {}", savedRide.getId());
        savedRide.setStatus(RideStatus.MATCHING);
        rideRepository.save(savedRide);
        return mapToResponse(savedRide);
    }

    public void updateRideWithDriver(String rideId, String driverId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));

        if (ride.getStatus() == RideStatus.CANCELLED || ride.getStatus() == RideStatus.COMPLETED) {
            log.warn("Ignoring matching result for ride {} in status {}", rideId, ride.getStatus());
            return;
        }

        com.flashCab.driverService.dto.DriverResponse driver;
        try {
            driver = driverServiceClient.getDriverById(driverId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matched driver not found");
        } catch (FeignException e) {
            log.error("Driver Service unavailable while processing matched ride {}: {}", rideId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Driver Service is unavailable");
        }

        if (driver == null || driver.getVehicleType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Matched driver is invalid or missing vehicle type");
        }

        if (ride.getVehicleType() != null && !driver.getVehicleType().name().equals(ride.getVehicleType().name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Matched driver vehicle type does not match the requested vehicle type");
        }

        ride.setDriverId(driverId);
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setActualFare(ride.getEstimatedFare());
        rideRepository.save(ride);
    }

    public RideResponse startRide(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ride cannot be started, Current status: " + ride.getStatus());
        }
        ride.setStatus(RideStatus.RIDE_STARTED);
        ride.setStartedAt(LocalDateTime.now());
        rideRepository.save(ride);
        return mapToResponse(ride);
    }

    public RideResponse completeRide(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (ride.getStatus() != RideStatus.RIDE_STARTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ride cannot be completed, Current status: " + ride.getStatus());
        }
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());
        ride.setActualFare(ride.getActualFare() != null ? ride.getActualFare() : ride.getEstimatedFare());
        rideRepository.save(ride);
        return mapToResponse(ride);
    }

    public RideResponse cancelRide(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ride cannot be cancelled from current status: " + ride.getStatus());
        }
        ride.setStatus(RideStatus.CANCELLED);
        rideRepository.save(ride);
        return mapToResponse(ride);
    }

    public RideResponse getRideById(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ride not found"));
        return mapToResponse(ride);
    }

    public List<RideResponse> getRidesByRider(String riderId) {
        return rideRepository.findByRiderIdOrderByCreatedAtDesc(riderId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void validateRiderExists(String riderId) {
        try {
            riderServiceClient.getRiderById(riderId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rider does not exist: " + riderId);
        } catch (FeignException e) {
            log.error("Unable to validate rider {} from Rider Service: {}", riderId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Rider Service is unavailable");
        }
    }

    private Double calculateEstimateFare(RideRequest request, LocalTime bookingTime) {
        double distanceKm = calculateDistanceKm(
                request.getPickupLatitude(),
                request.getPickupLongitude(),
                request.getDropLatitude(),
                request.getDropLongitude());
        double baseFare = calculateBaseFare(request.getVehicleType(), distanceKm);
        double timeMultiplier = calculateTimeMultiplier(bookingTime);
        double weatherMultiplier = calculateWeatherMultiplier(
                request.getPickupLatitude(),
                request.getPickupLongitude());
        return roundFare(baseFare * timeMultiplier * weatherMultiplier);
    }

    private double calculateDistanceKm(double pickupLatitude, double pickupLongitude, double dropLatitude,
            double dropLongitude) {
        double lat1 = Math.toRadians(pickupLatitude);
        double lat2 = Math.toRadians(dropLatitude);
        double lon1 = Math.toRadians(pickupLongitude);
        double lon2 = Math.toRadians(dropLongitude);
        double latDiff = lat2 - lat1;
        double lonDiff = lon1 - lon2;

        double a = Math.pow(Math.sin(latDiff / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(lonDiff / 2), 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return 6371 * c;
    }

    private double calculateBaseFare(VehicleType vehicleType, double distanceKm) {
        return vehicleFarePolicy.getBaseFare(vehicleType)
                + (distanceKm * vehicleFarePolicy.getPerKmRate(vehicleType));
    }

    private double calculateTimeMultiplier(LocalTime bookingTime) {
        if (isInTimeRange(bookingTime, LocalTime.of(5, 0), LocalTime.of(8, 0))
                || isInTimeRange(bookingTime, LocalTime.of(11, 0), LocalTime.of(17, 0))) {
            return 1.0;
        }
        if (isInTimeRange(bookingTime, LocalTime.of(8, 0), LocalTime.of(11, 0))
                || isInTimeRange(bookingTime, LocalTime.of(17, 0), LocalTime.of(22, 0))) {
            return 1.20;
        }
        return 1.15;
    }

    private boolean isInTimeRange(LocalTime time, LocalTime startInclusive, LocalTime endExclusive) {
        return !time.isBefore(startInclusive) && time.isBefore(endExclusive);
    }

    private double calculateWeatherMultiplier(double pickupLatitude, double pickupLongitude) {
        try {
            OpenMeteoWeatherResponse response = openMeteoWeatherClient.getCurrentWeather(
                    pickupLatitude,
                    pickupLongitude);
            if (response == null || response.getCurrent() == null) {
                throw new IllegalStateException("Open-Meteo response is missing current weather data");
            }

            OpenMeteoWeatherResponse.Current current = response.getCurrent();
            if (current.getWeatherCode() == null
                    || current.getPrecipitation() == null
                    || current.getRain() == null
                    || !Double.isFinite(current.getPrecipitation())
                    || !Double.isFinite(current.getRain())
                    || current.getPrecipitation() < 0
                    || current.getRain() < 0) {
                throw new IllegalStateException("Open-Meteo response contains invalid weather data");
            }

            return classifyWeatherMultiplier(
                    current.getWeatherCode(),
                    current.getPrecipitation(),
                    current.getRain());
        } catch (RestClientException | IllegalArgumentException | IllegalStateException exception) {
            log.warn(
                    "Unable to retrieve valid Open-Meteo weather for pickup ({}, {}); using neutral weather multiplier",
                    pickupLatitude,
                    pickupLongitude,
                    exception);
            return 1.0;
        }
    }

    private double classifyWeatherMultiplier(int weatherCode, double precipitation, double rain) {
        if (weatherCode == 95 || weatherCode == 96 || weatherCode == 99) {
            return 1.30;
        }
        if (weatherCode == 56 || weatherCode == 57
                || weatherCode == 66 || weatherCode == 67
                || weatherCode == 71 || weatherCode == 73 || weatherCode == 75
                || weatherCode == 77 || weatherCode == 85 || weatherCode == 86) {
            return 1.25;
        }

        double precipitationMultiplier = calculatePrecipitationMultiplier(precipitation, rain);
        return switch (weatherCode) {
            case 0, 1, 2, 3, 45, 48 -> precipitationMultiplier;
            case 51, 61, 80 -> Math.max(1.10, precipitationMultiplier);
            case 53, 55, 63, 81 -> Math.max(1.15, precipitationMultiplier);
            case 65, 82 -> 1.25;
            default -> throw new IllegalArgumentException("Unrecognized Open-Meteo weather code: " + weatherCode);
        };
    }

    private double calculatePrecipitationMultiplier(double precipitation, double rain) {
        double amount = Math.max(precipitation, rain);
        if (amount >= 7.6) {
            return 1.25;
        }
        if (amount >= 2.5) {
            return 1.15;
        }
        return amount > 0 ? 1.10 : 1.0;
    }

    private double roundFare(double fare) {
        return Math.round(fare * 100.0) / 100.0;
    }

    private RideResponse mapToResponse(Ride savedRide) {
        RideResponse response = new RideResponse();

        response.setId(savedRide.getId());
        response.setRiderId(savedRide.getRiderId());
        response.setDriverId(savedRide.getDriverId());
        response.setVehicleType(savedRide.getVehicleType());

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
