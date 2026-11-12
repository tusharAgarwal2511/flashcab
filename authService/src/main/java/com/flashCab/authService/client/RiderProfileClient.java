package com.flashCab.authService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "riderService", url = "${auth.rider-service.url}")
public interface RiderProfileClient {

    @PostMapping("/internal/riders")
    ProfileResponse provisionRider(@RequestBody RiderProfileProvisionRequest request);
}
