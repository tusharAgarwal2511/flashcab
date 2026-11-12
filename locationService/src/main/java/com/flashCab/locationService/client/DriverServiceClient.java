package com.flashCab.locationService.client;

import com.flashCab.driverService.dto.DriverResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "driverService", url = "${driver.service.url}")
public interface DriverServiceClient {

    @GetMapping("/internal/drivers/{driverId}")
    DriverResponse getDriverById(@PathVariable("driverId") String driverId);

    @GetMapping("/internal/drivers/by-auth-user/{authUserId}")
    DriverResponse getDriverByAuthUserId(@PathVariable("authUserId") String authUserId);

    @GetMapping("/internal/drivers")
    List<DriverResponse> getDrivers();
}
