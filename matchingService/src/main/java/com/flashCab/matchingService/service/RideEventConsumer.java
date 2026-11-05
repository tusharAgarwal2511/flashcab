package com.flashCab.matchingService.service;

import com.flashCab.matchingService.event.RideRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class RideEventConsumer {
    private final MatchingService matchingService;

    @KafkaListener(topics = "ride.requested", groupId = "matching-service-group")
    public void consumeRideRequestedEvent(RideRequestedEvent event) {
        try {
            matchingService.matchDriverForRide(event);
        } catch (Exception e) {
            log.error("Error processing ride request: {} - {}", event.getRideId(), e.getMessage());
            // in prod send to dead letter queue for retry
        }
    }

}
