package com.flashCab.driverService.repository;

import com.flashCab.driverService.model.Driver;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DriverRepository extends MongoRepository<Driver, String> {
}
