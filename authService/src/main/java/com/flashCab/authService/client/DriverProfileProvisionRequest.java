package com.flashCab.authService.client;

import com.flashCab.authService.model.VehicleType;

public record DriverProfileProvisionRequest(
        String authUserId,
        String name,
        String email,
        String phone,
        VehicleType vehicleType,
        String vehicleNumber) {
}
