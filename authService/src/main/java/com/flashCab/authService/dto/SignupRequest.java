package com.flashCab.authService.dto;

import com.flashCab.authService.model.UserRole;
import com.flashCab.authService.model.VehicleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank String phone,
        @NotNull UserRole role,
        VehicleType vehicleType,
        String vehicleNumber) {
}
