package com.flashCab.matchingService.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideRequestedEvent {

    private String riderId;

    private String rideId;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private String pickupAddress;

    private Double dropLatitude;

    private Double dropLongitude;

    private String dropAddress;
}
