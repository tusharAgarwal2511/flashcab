package com.flashCab.driverService.repository;

import com.flashCab.driverService.model.Driver;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DriverRepository extends MongoRepository<Driver, String> {
    Optional<Driver> findByAuthUserId(String authUserId);

    boolean existsByAuthUserId(String authUserId);
}
