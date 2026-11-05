package com.flashCab.rideService.dto;

import com.flashCab.rideService.model.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideResponse {
    private String id;

    private String riderId;

    private String driverId;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private String pickupAddress;

    private Double dropLatitude;

    private Double dropLongitude;

    private String dropAddress;

    private RideStatus status;

    private Double estimatedFare;

    private Double actualFare;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}
