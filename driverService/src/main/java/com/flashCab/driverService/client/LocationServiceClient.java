package com.flashCab.driverService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "locationService", url = "${location.service.url}")
public interface LocationServiceClient {

    @DeleteMapping("/api/v1/locations/drivers/{driverId}")
    void removeDriverLocation(@PathVariable("driverId") String driverId);
}
