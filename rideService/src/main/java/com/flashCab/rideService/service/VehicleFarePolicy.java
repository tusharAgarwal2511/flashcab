package com.flashCab.rideService.service;

import com.flashCab.rideService.model.VehicleType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VehicleFarePolicy {

    @Value("${fare.vehicle.MINI.base-fare:40.0}")
    private double miniBaseFare;

    @Value("${fare.vehicle.MINI.per-km-rate:10.0}")
    private double miniPerKmRate;

    @Value("${fare.vehicle.SEDAN.base-fare:50.0}")
    private double sedanBaseFare;

    @Value("${fare.vehicle.SEDAN.per-km-rate:12.0}")
    private double sedanPerKmRate;

    @Value("${fare.vehicle.SUV.base-fare:80.0}")
    private double suvBaseFare;

    @Value("${fare.vehicle.SUV.per-km-rate:18.0}")
    private double suvPerKmRate;

    @Value("${fare.vehicle.PREMIUM.base-fare:120.0}")
    private double premiumBaseFare;

    @Value("${fare.vehicle.PREMIUM.per-km-rate:25.0}")
    private double premiumPerKmRate;

    @Value("${fare.vehicle.BIKE.base-fare:20.0}")
    private double bikeBaseFare;

    @Value("${fare.vehicle.BIKE.per-km-rate:6.0}")
    private double bikePerKmRate;

    public double getBaseFare(VehicleType vehicleType) {
        return switch (vehicleType) {
            case MINI -> miniBaseFare;
            case SEDAN -> sedanBaseFare;
            case SUV -> suvBaseFare;
            case PREMIUM -> premiumBaseFare;
            case BIKE -> bikeBaseFare;
        };
    }

    public double getPerKmRate(VehicleType vehicleType) {
        return switch (vehicleType) {
            case MINI -> miniPerKmRate;
            case SEDAN -> sedanPerKmRate;
            case SUV -> suvPerKmRate;
            case PREMIUM -> premiumPerKmRate;
            case BIKE -> bikePerKmRate;
        };
    }
}
