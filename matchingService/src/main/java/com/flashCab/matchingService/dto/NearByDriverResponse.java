package com.flashCab.matchingService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearByDriverResponse {

    private String driverId;
    private Double latitude;
    private Double longitude;
    private Double distanceInKm;

}
