package com.flashCab.rideService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "locationService", url = "${location.service.url}")
public interface LocationServiceClient {

    @GetMapping("/api/v1/locations/drivers/nearby")
    List<Map<String, Object>> getNearByDrivers(@RequestParam double latitude,
                                               @RequestParam double longitude,
                                               @RequestParam double radiusInKm);

    @PostMapping("/api/v1/locations/drivers/{driverId}/release")
    boolean releaseDriver(@PathVariable("driverId") String driverId, @RequestParam("rideId") String rideId);
}
