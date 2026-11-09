package com.flashCab.rideService;

import com.flashCab.rideService.client.DriverServiceClient;
import com.flashCab.rideService.client.LocationServiceClient;
import com.flashCab.rideService.client.OpenMeteoWeatherClient;
import com.flashCab.rideService.client.RiderServiceClient;
import com.flashCab.rideService.event.RideRequestedEvent;
import com.flashCab.rideService.model.Ride;
import com.flashCab.rideService.model.RideStatus;
import com.flashCab.rideService.repository.RideRepository;
import com.flashCab.rideService.service.RideService;
import com.flashCab.rideService.service.VehicleFarePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RideDriverAvailabilityTests {

    private final RideRepository rideRepository = mock(RideRepository.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, RideRequestedEvent> kafkaTemplate = mock(KafkaTemplate.class);
    private final RiderServiceClient riderServiceClient = mock(RiderServiceClient.class);
    private final DriverServiceClient driverServiceClient = mock(DriverServiceClient.class);
    private final LocationServiceClient locationServiceClient = mock(LocationServiceClient.class);
    private final VehicleFarePolicy vehicleFarePolicy = mock(VehicleFarePolicy.class);
    private final OpenMeteoWeatherClient openMeteoWeatherClient = mock(OpenMeteoWeatherClient.class);
    private final RideService rideService = new RideService(
            rideRepository,
            kafkaTemplate,
            riderServiceClient,
            driverServiceClient,
            locationServiceClient,
            vehicleFarePolicy,
            openMeteoWeatherClient);

    @Test
    void completingRideReleasesItsAssignedDriver() {
        Ride ride = rideWithStatus(RideStatus.RIDE_STARTED, "driver-1");
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(locationServiceClient.releaseDriver("driver-1", "ride-1")).thenReturn(true);

        rideService.completeRide("ride-1");

        verify(locationServiceClient).releaseDriver("driver-1", "ride-1");
    }

    @Test
    void cancellingUnmatchedRideDoesNotReleaseAnyDriver() {
        Ride ride = rideWithStatus(RideStatus.MATCHING, null);
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        rideService.cancelRide("ride-1");

        verifyNoInteractions(locationServiceClient);
    }

    @Test
    void cancellingAssignedRideReleasesOnlyItsReservation() {
        Ride ride = rideWithStatus(RideStatus.ACCEPTED, "driver-1");
        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(locationServiceClient.releaseDriver("driver-1", "ride-1")).thenReturn(true);

        rideService.cancelRide("ride-1");

        verify(locationServiceClient).releaseDriver("driver-1", "ride-1");
        verifyNoMoreInteractions(locationServiceClient);
    }

    private Ride rideWithStatus(RideStatus status, String driverId) {
        Ride ride = new Ride();
        ride.setId("ride-1");
        ride.setStatus(status);
        ride.setDriverId(driverId);
        return ride;
    }
}
