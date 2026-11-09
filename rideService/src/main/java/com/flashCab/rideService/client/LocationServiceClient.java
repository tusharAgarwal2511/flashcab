package com.flashCab.rideService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "locationService", url = "${location.service.url}")
public interface LocationServiceClient {

    @PostMapping("/api/v1/locations/drivers/{driverId}/release")
    boolean releaseDriver(@PathVariable("driverId") String driverId, @RequestParam("rideId") String rideId);
}
