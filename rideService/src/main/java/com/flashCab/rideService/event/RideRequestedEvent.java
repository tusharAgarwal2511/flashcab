package com.flashCab.rideService.event;

import com.flashCab.rideService.model.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideRequestedEvent {
    private String rideId;

    private String riderId;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private String pickupAddress;

    private Double dropLatitude;

    private Double dropLongitude;

    private String dropAddress;

    private VehicleType vehicleType;
}
