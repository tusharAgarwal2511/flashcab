package com.flashCab.authService.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
public class AuthUser {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private UserRole role;

    private AccountStatus status;

    private String name;

    private String phone;

    private VehicleType vehicleType;

    private String vehicleNumber;

    private String profileId;

    private Instant createdAt;
}
