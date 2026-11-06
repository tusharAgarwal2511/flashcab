package com.flashCab.riderService.service;

import com.flashCab.riderService.dto.RiderRequest;
import com.flashCab.riderService.dto.RiderResponse;
import com.flashCab.riderService.model.Rider;
import com.flashCab.riderService.repository.RiderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiderService {

    private final RiderRepository riderRepository;

    public RiderResponse registerRider(RiderRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Rider rider = new Rider();
        rider.setId(UUID.randomUUID().toString());
        applyRequest(rider, request);
        rider.setCreatedAt(now);
        rider.setUpdatedAt(now);
        return mapToResponse(riderRepository.save(rider));
    }

    public List<RiderResponse> getAllRiders() {
        return riderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RiderResponse getRiderById(String riderId) {
        return mapToResponse(findRiderById(riderId));
    }

    public RiderResponse updateRider(String riderId, RiderRequest request) {
        Rider rider = findRiderById(riderId);
        applyRequest(rider, request);
        rider.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(riderRepository.save(rider));
    }

    public void deleteRider(String riderId) {
        riderRepository.delete(findRiderById(riderId));
    }

    private Rider findRiderById(String riderId) {
        return riderRepository.findById(riderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rider not found"));
    }

    private void applyRequest(Rider rider, RiderRequest request) {
        rider.setName(request.getName());
        rider.setEmail(request.getEmail());
        rider.setPhone(request.getPhone());
    }

    private RiderResponse mapToResponse(Rider rider) {
        return new RiderResponse(
                rider.getId(),
                rider.getName(),
                rider.getEmail(),
                rider.getPhone(),
                rider.getCreatedAt(),
                rider.getUpdatedAt()
        );
    }
}
