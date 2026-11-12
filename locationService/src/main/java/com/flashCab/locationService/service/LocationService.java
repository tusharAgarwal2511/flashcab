package com.flashCab.locationService.service;

import com.flashCab.driverService.dto.DriverResponse;
import com.flashCab.locationService.client.DriverServiceClient;
import com.flashCab.locationService.dto.DriverLocationRequest;
import com.flashCab.locationService.dto.NearByDriverResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationService {

    private static final String DRIVERS_GEO_KEY = "drivers.locations";
    private static final String DRIVER_AVAILABILITY_KEY = "drivers.availability";
    private static final String DRIVER_RESERVATIONS_KEY = "drivers.reservations";
    private static final DefaultRedisScript<Long> RESERVE_DRIVER_SCRIPT = new DefaultRedisScript<>("""
            local availability = redis.call('HGET', KEYS[1], ARGV[1])
            if not availability then
                availability = 'true'
                redis.call('HSET', KEYS[1], ARGV[1], availability)
            end
            if availability ~= 'true' or redis.call('HEXISTS', KEYS[2], ARGV[1]) == 1 then
                return 0
            end
            redis.call('HSET', KEYS[1], ARGV[1], 'false')
            redis.call('HSET', KEYS[2], ARGV[1], ARGV[2])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> RELEASE_DRIVER_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('HGET', KEYS[2], ARGV[1]) ~= ARGV[2] then
                return 0
            end
            redis.call('HSET', KEYS[1], ARGV[1], 'true')
            redis.call('HDEL', KEYS[2], ARGV[1])
            return 1
            """, Long.class);
    private final RedisTemplate<String, String> redisTemplate;
    private final DriverServiceClient driverServiceClient;

    public void updateDriverLocation(DriverLocationRequest driverLocationRequest, String authUserId) {
        if (driverLocationRequest == null || driverLocationRequest.getDriverId() == null || driverLocationRequest.getDriverId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver ID is required");
        }
        if (authUserId == null || authUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }
        DriverResponse driver;
        try {
            driver = driverServiceClient.getDriverByAuthUserId(authUserId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No driver profile belongs to this account");
        } catch (FeignException e) {
            log.error("Driver validation failed for authenticated user {}: {}", authUserId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Driver Service is unavailable");
        }
        if (driver == null || driver.getId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No driver profile belongs to this account");
        }
        if (!driver.getId().equals(driverLocationRequest.getDriverId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Driver ID does not belong to this account");
        }
        String driverId = driver.getId();
        log.info("Updating location for driver: {}", driverId);
        Point driverPoint = new Point(driverLocationRequest.getLongitude(), driverLocationRequest.getLatitude());
        redisTemplate.opsForGeo().add(DRIVERS_GEO_KEY, driverPoint, driverId);
        initializeAvailability(driverId);
        log.info("Location updated for driver: {}", driverId);
    }

    public int seedDrivers(int count, double minLat, double maxLat, double minLon, double maxLon) {
        if (count <= 0) {
            return 0;
        }

        List<DriverResponse> drivers;
        try {
            drivers = driverServiceClient.getDrivers();
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No registered drivers available to seed");
        } catch (FeignException e) {
            log.error("Unable to fetch registered drivers for seeding: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Driver Service is unavailable");
        }

        if (drivers == null || drivers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No registered drivers available to seed");
        }

        List<String> driverIds = drivers.stream().map(DriverResponse::getId).filter(id -> id != null && !id.isBlank()).distinct().toList();
        int availableCount = Math.min(count, driverIds.size());
        if (availableCount <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No registered drivers available to seed");
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < availableCount; i++) {
            String driverId = driverIds.get(i);
            double latitude = random.nextDouble(minLat, maxLat);
            double longitude = random.nextDouble(minLon, maxLon);
            Point point = new Point(longitude, latitude);
            redisTemplate.opsForGeo().add(DRIVERS_GEO_KEY, point, driverId);
            initializeAvailability(driverId);
        }
        return availableCount;
    }

    public List<NearByDriverResponse> findNearByDrivers(double latitude, double longitude, double radiusInKm) {
        log.info("Finding drivers near lat: {} long: {} within radius: {}km", latitude, longitude, radiusInKm);
        Circle searchArea = new Circle(new Point(longitude, latitude), new Distance(radiusInKm, Metrics.KILOMETERS));
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().radius(
                DRIVERS_GEO_KEY,
                searchArea,
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .includeCoordinates()
                        .includeDistance()
                        .sortAscending()
        );

        List<NearByDriverResponse> nearByDrivers = new ArrayList<>();
        if (results != null) {
            results.getContent().forEach(result -> {
                RedisGeoCommands.GeoLocation<String> location = result.getContent();
                if (nearByDrivers.size() < 10 && isDriverAvailable(location.getName())) {
                    nearByDrivers.add(new NearByDriverResponse(
                            location.getName(),
                            location.getPoint().getY(),
                            location.getPoint().getX(),
                            result.getDistance().getValue()
                    ));
                }
            });
        }
        log.info("Found {} drivers nearby", nearByDrivers.size());
        return nearByDrivers;
    }

    public boolean reserveDriver(String driverId, String rideId) {
        validateReservationIds(driverId, rideId);
        Long reserved = redisTemplate.execute(
                RESERVE_DRIVER_SCRIPT,
                List.of(DRIVER_AVAILABILITY_KEY, DRIVER_RESERVATIONS_KEY),
                driverId,
                rideId
        );
        return Objects.equals(reserved, 1L);
    }

    public boolean releaseDriver(String driverId, String rideId) {
        validateReservationIds(driverId, rideId);
        Long released = redisTemplate.execute(
                RELEASE_DRIVER_SCRIPT,
                List.of(DRIVER_AVAILABILITY_KEY, DRIVER_RESERVATIONS_KEY),
                driverId,
                rideId
        );
        return Objects.equals(released, 1L);
    }

    public void removeDriver(String driverId) {
        log.info("Removing driver: {}", driverId);
        redisTemplate.opsForGeo().remove(DRIVERS_GEO_KEY, driverId);
    }

    private void initializeAvailability(String driverId) {
        redisTemplate.opsForHash().putIfAbsent(DRIVER_AVAILABILITY_KEY, driverId, "true");
    }

    private boolean isDriverAvailable(String driverId) {
        initializeAvailability(driverId);
        return "true".equals(redisTemplate.opsForHash().get(DRIVER_AVAILABILITY_KEY, driverId));
    }

    private void validateReservationIds(String driverId, String rideId) {
        if (driverId == null || driverId.isBlank() || rideId == null || rideId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver ID and ride ID are required");
        }
    }

}
