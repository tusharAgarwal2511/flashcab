package com.flashCab.rideService.service;

import com.flashCab.rideService.client.DriverServiceClient;
import com.flashCab.rideService.client.LocationServiceClient;
import com.flashCab.rideService.client.OpenMeteoWeatherClient;
import com.flashCab.rideService.client.RiderServiceClient;
import com.flashCab.rideService.dto.OpenMeteoWeatherResponse;
import com.flashCab.rideService.dto.RideRequest;
import com.flashCab.rideService.model.VehicleType;
import com.flashCab.rideService.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RideServiceFareEstimateTests {

    private final VehicleFarePolicy vehicleFarePolicy = mock(VehicleFarePolicy.class);
    private final OpenMeteoWeatherClient weatherClient = mock(OpenMeteoWeatherClient.class);
    private final RideService rideService = new RideService(
            mock(RideRepository.class),
            mock(KafkaTemplate.class),
            mock(RiderServiceClient.class),
            mock(DriverServiceClient.class),
            mock(LocationServiceClient.class),
            vehicleFarePolicy,
            weatherClient);

    @BeforeEach
    void setUp() {
        when(vehicleFarePolicy.getBaseFare(VehicleType.SEDAN)).thenReturn(50.0);
        when(vehicleFarePolicy.getPerKmRate(VehicleType.SEDAN)).thenReturn(12.0);
        when(vehicleFarePolicy.getBaseFare(VehicleType.BIKE)).thenReturn(20.0);
        when(vehicleFarePolicy.getPerKmRate(VehicleType.BIKE)).thenReturn(6.0);

        OpenMeteoWeatherResponse.Current current = new OpenMeteoWeatherResponse.Current();
        current.setWeatherCode(0);
        current.setPrecipitation(0.0);
        current.setRain(0.0);
        OpenMeteoWeatherResponse weather = new OpenMeteoWeatherResponse();
        weather.setCurrent(current);
        when(weatherClient.getCurrentWeather(0.0, 0.0)).thenReturn(weather);
    }

    @Test
    void calculatesExpectedFareForSedanRide() {
        double fare = estimateFare(VehicleType.SEDAN);

        assertEquals(1384.34, fare, 0.01);
    }

    @Test
    void calculatesExpectedFareForBikeRide() {
        double fare = estimateFare(VehicleType.BIKE);

        assertEquals(687.17, fare, 0.01);
    }

    private double estimateFare(VehicleType vehicleType) {
        RideRequest request = new RideRequest();
        request.setPickupLatitude(0.0);
        request.setPickupLongitude(0.0);
        request.setDropLatitude(0.0);
        request.setDropLongitude(1.0);
        request.setVehicleType(vehicleType);

        return ReflectionTestUtils.invokeMethod(
                rideService,
                "calculateEstimateFare",
                request,
                LocalTime.of(12, 0));
    }
}
