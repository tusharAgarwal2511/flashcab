package com.flashCab.driverService.dto;

import com.flashCab.driverService.model.VehicleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DriverProfileProvisionRequest {

    @NotBlank
    private String authUserId;

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String phone;

    @NotNull
    private VehicleType vehicleType;

    @NotBlank
    private String vehicleNumber;
}
