package com.flashCab.rideService.client;

import com.flashCab.riderService.dto.RiderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "riderService", url = "${rider.service.url}")
public interface RiderServiceClient {

    @GetMapping("/api/v1/riders/{riderId}")
    RiderResponse getRiderById(@PathVariable("riderId") String riderId);
}
