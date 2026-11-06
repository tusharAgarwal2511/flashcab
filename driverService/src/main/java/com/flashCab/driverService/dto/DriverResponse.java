package com.flashCab.driverService.dto;

import com.flashCab.driverService.model.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverResponse {

    private String id;

    private String name;

    private String email;

    private String phone;

    private VehicleType vehicleType;

    private String vehicleNumber;

    private LocalDateTime createdAt;
}
