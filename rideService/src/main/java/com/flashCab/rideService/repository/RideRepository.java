package com.flashCab.rideService.repository;

import com.flashCab.rideService.model.Ride;
import com.flashCab.rideService.model.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, String> {
    List<Ride> findByRiderIdOrderByCreatedAtDesc(String riderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Ride r
            SET r.driverId = :driverId,
                r.status = :acceptedStatus
            WHERE r.id = :rideId
              AND r.status IN (:requestedStatus, :matchingStatus)
              AND r.driverId IS NULL
            """)
    int assignDriverIfMatching(
            @Param("rideId") String rideId,
            @Param("driverId") String driverId,
            @Param("requestedStatus") RideStatus requestedStatus,
            @Param("matchingStatus") RideStatus matchingStatus,
            @Param("acceptedStatus") RideStatus acceptedStatus
    );
}
