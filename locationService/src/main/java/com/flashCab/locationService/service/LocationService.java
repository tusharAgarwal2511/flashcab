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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationService {

    private static final String DRIVERS_GEO_KEY = "drivers.locations";
    private final RedisTemplate<String, String> redisTemplate;
    private final DriverServiceClient driverServiceClient;

    public void updateDriverLocation(DriverLocationRequest driverLocationRequest) {
        if (driverLocationRequest == null || driverLocationRequest.getDriverId() == null || driverLocationRequest.getDriverId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver ID is required");
        }
        try {
            DriverResponse driver = driverServiceClient.getDriverById(driverLocationRequest.getDriverId());
            if (driver == null || driver.getId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver does not exist");
            }
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver does not exist");
        } catch (FeignException e) {
            log.error("Driver validation failed for {}: {}", driverLocationRequest.getDriverId(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Driver Service is unavailable");
        }
        log.info("Updating location for driver: {}", driverLocationRequest.getDriverId());
        Point driverPoint = new Point(driverLocationRequest.getLongitude(), driverLocationRequest.getLatitude());
        redisTemplate.opsForGeo().add(DRIVERS_GEO_KEY, driverPoint, driverLocationRequest.getDriverId());
        log.info("Location updated for driver: {}", driverLocationRequest.getDriverId());
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
                        .limit(10)
        );

        List<NearByDriverResponse> nearByDrivers = new ArrayList<>();
        if (results != null) {
            results.getContent().forEach(result -> {
                RedisGeoCommands.GeoLocation<String> location = result.getContent();
                nearByDrivers.add(new NearByDriverResponse(
                        location.getName(),
                        location.getPoint().getY(),
                        location.getPoint().getX(),
                        result.getDistance().getValue()
                ));
            });
        }
        log.info("Found {} drivers nearby", nearByDrivers.size());
        return nearByDrivers;
    }

    public void removeDriver(String driverId) {
        log.info("Removing driver: {}", driverId);
        redisTemplate.opsForGeo().remove(DRIVERS_GEO_KEY, driverId);
    }

}
