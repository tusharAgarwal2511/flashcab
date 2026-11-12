package com.flashCab.rideService.client;

import com.flashCab.riderService.dto.RiderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "riderService", url = "${rider.service.url}")
public interface RiderServiceClient {

    @GetMapping("/internal/riders/{riderId}")
    RiderResponse getRiderById(@PathVariable("riderId") String riderId);

    @GetMapping("/internal/riders/by-auth-user/{authUserId}")
    RiderResponse getRiderByAuthUserId(@PathVariable("authUserId") String authUserId);
}
