package com.flashCab.rideService.dto;

import com.flashCab.rideService.model.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideRequest {

    @NotBlank(message = "Rider id is required")
    private String riderId;

    @NotNull(message = "Pickup latitude id is required")
    private Double pickupLatitude;

    @NotNull(message = "Pickup longitude id is required")
    private Double pickupLongitude;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotNull(message = "Drop latitude id is required")
    private Double dropLatitude;

    @NotNull(message = "Drop longitude id is required")
    private Double dropLongitude;

    @NotBlank(message = "Drop address is required")
    private String dropAddress;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;
}
