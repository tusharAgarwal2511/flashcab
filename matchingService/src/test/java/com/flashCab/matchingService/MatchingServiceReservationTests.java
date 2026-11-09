package com.flashCab.matchingService;

import com.flashCab.matchingService.client.DriverServiceClient;
import com.flashCab.matchingService.client.LocationServiceClient;
import com.flashCab.matchingService.dto.DriverProfileResponse;
import com.flashCab.matchingService.dto.NearByDriverResponse;
import com.flashCab.matchingService.event.RideMatchedEvent;
import com.flashCab.matchingService.event.RideRequestedEvent;
import com.flashCab.matchingService.service.MatchingService;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MatchingServiceReservationTests {

    private final LocationServiceClient locationServiceClient = mock(LocationServiceClient.class);
    private final DriverServiceClient driverServiceClient = mock(DriverServiceClient.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, RideMatchedEvent> kafkaTemplate = mock(KafkaTemplate.class);
    private final MatchingService matchingService =
            new MatchingService(locationServiceClient, driverServiceClient, kafkaTemplate);

    @Test
    void triesNextRankedCandidateWhenReservationFails() {
        when(locationServiceClient.getNearByDrivers(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(nearbyDrivers());
        when(driverServiceClient.getDriversByIds(anyList())).thenReturn(driverProfiles());
        when(locationServiceClient.reserveDriver(anyString(), eq("ride-1"))).thenReturn(false, true);

        matchingService.matchDriverForRide(rideRequestedEvent());

        verify(locationServiceClient, times(2)).reserveDriver(anyString(), eq("ride-1"));
        verify(kafkaTemplate).send(eq("ride.matched"), eq("ride-1"), any(RideMatchedEvent.class));
    }

    @Test
    void doesNotPublishWhenNoCandidateCanBeReserved() {
        when(locationServiceClient.getNearByDrivers(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(nearbyDrivers());
        when(driverServiceClient.getDriversByIds(anyList())).thenReturn(driverProfiles());
        when(locationServiceClient.reserveDriver(anyString(), eq("ride-1"))).thenReturn(false);

        matchingService.matchDriverForRide(rideRequestedEvent());

        verify(locationServiceClient, times(2)).reserveDriver(anyString(), eq("ride-1"));
        verifyNoInteractions(kafkaTemplate);
    }

    private List<NearByDriverResponse> nearbyDrivers() {
        return List.of(
                new NearByDriverResponse("driver-1", 1.0, 1.0, 1.0),
                new NearByDriverResponse("driver-2", 2.0, 2.0, 5.0)
        );
    }

    private List<DriverProfileResponse> driverProfiles() {
        return List.of(
                new DriverProfileResponse("driver-1", "One", "one@example.com", "1", "SEDAN", "A"),
                new DriverProfileResponse("driver-2", "Two", "two@example.com", "2", "SEDAN", "B")
        );
    }

    private RideRequestedEvent rideRequestedEvent() {
        return new RideRequestedEvent("rider-1", "ride-1", 1.0, 1.0, "pickup",
                2.0, 2.0, "drop", "SEDAN");
    }
}
