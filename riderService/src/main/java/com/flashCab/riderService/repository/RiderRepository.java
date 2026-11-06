package com.flashCab.riderService.repository;

import com.flashCab.riderService.model.Rider;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RiderRepository extends MongoRepository<Rider, String> {
}
