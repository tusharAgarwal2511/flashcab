package com.flashCab.driverService.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "drivers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Driver {

    @Id
    private String id;

    private String name;

    private String email;

    private String phone;

    private VehicleType vehicleType;

    private String vehicleNumber;

    private LocalDateTime createdAt;
}
