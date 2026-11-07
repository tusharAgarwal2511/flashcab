package com.flashCab.matchingService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverProfileResponse {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String vehicleType;
    private String vehicleNumber;
}
