package com.flashCab.locationService.service;

import com.flashCab.locationService.client.DriverServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocationServiceReservationTests {

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    private final LocationService locationService =
            new LocationService(redisTemplate, mock(DriverServiceClient.class));

    @Test
    void reportsSuccessfulReservation() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), eq("driver-1"), eq("ride-1")))
                .thenReturn(1L);

        assertTrue(locationService.reserveDriver("driver-1", "ride-1"));
    }

    @Test
    void reportsUnavailableReservation() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), eq("driver-1"), eq("ride-2")))
                .thenReturn(0L);

        assertFalse(locationService.reserveDriver("driver-1", "ride-2"));
    }

    @Test
    void releaseOperationIncludesTheOwningRideId() {
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("drivers.availability", "drivers.reservations")),
                eq("driver-1"), eq("ride-1"))).thenReturn(1L);

        assertTrue(locationService.releaseDriver("driver-1", "ride-1"));
    }
}
