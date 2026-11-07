package com.flashCab.matchingService.client;

import com.flashCab.matchingService.dto.DriverProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "driverService", url = "${driver.service.url}")
public interface DriverServiceClient {

    @GetMapping("/api/v1/drivers/batch")
    List<DriverProfileResponse> getDriversByIds(@RequestParam("ids") List<String> ids);
}
