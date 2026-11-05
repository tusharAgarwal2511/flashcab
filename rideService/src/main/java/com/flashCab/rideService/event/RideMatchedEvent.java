package com.flashCab.rideService.event;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideMatchedEvent {

    private String rideId;

    private String riderId;

    private String driverId;

    private Double driverLatitude;

    private Double driverLongitude;

    private Double distanceToPickupKm;

}
