package com.flashCab.riderService.repository;

import com.flashCab.riderService.model.Rider;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RiderRepository extends MongoRepository<Rider, String> {
    Optional<Rider> findByAuthUserId(String authUserId);

    boolean existsByAuthUserId(String authUserId);
}
