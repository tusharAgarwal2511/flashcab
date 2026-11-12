package com.flashCab.authService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "driverService", url = "${auth.driver-service.url}")
public interface DriverProfileClient {

    @PostMapping("/internal/drivers")
    ProfileResponse provisionDriver(@RequestBody DriverProfileProvisionRequest request);
}
