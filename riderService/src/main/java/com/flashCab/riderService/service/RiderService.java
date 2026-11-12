package com.flashCab.riderService.service;

import com.flashCab.riderService.dto.RiderRequest;
import com.flashCab.riderService.dto.RiderProfileProvisionRequest;
import com.flashCab.riderService.dto.RiderResponse;
import com.flashCab.riderService.model.Rider;
import com.flashCab.riderService.repository.RiderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RiderService {

    private final RiderRepository riderRepository;

    public RiderResponse registerRider(RiderRequest request, String authUserId) {
        requireAuthUserId(authUserId);
        if (riderRepository.existsByAuthUserId(authUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rider profile already exists");
        }
        LocalDateTime now = LocalDateTime.now();
        Rider rider = new Rider();
        applyRequest(rider, request);
        rider.setAuthUserId(authUserId);
        rider.setCreatedAt(now);
        rider.setUpdatedAt(now);
        try {
            return mapToResponse(riderRepository.save(rider));
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rider profile already exists");
        }
    }

    public RiderResponse provisionRider(RiderProfileProvisionRequest request) {
        requireAuthUserId(request.getAuthUserId());
        return riderRepository.findByAuthUserId(request.getAuthUserId())
                .map(this::mapToResponse)
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    Rider rider = new Rider();
                    rider.setAuthUserId(request.getAuthUserId());
                    rider.setName(request.getName());
                    rider.setEmail(request.getEmail());
                    rider.setPhone(request.getPhone());
                    rider.setCreatedAt(now);
                    rider.setUpdatedAt(now);
                    try {
                        return mapToResponse(riderRepository.save(rider));
                    } catch (DuplicateKeyException exception) {
                        return riderRepository.findByAuthUserId(request.getAuthUserId())
                                .map(this::mapToResponse)
                                .orElseThrow(() -> exception);
                    }
                });
    }

    public List<RiderResponse> getRidersForUser(String authUserId) {
        return List.of(getRiderByAuthUserId(authUserId));
    }

    public RiderResponse getRiderById(String riderId, String authUserId) {
        Rider rider = findRiderById(riderId);
        requireOwner(rider, authUserId);
        return mapToResponse(rider);
    }

    public RiderResponse getInternalRiderById(String riderId) {
        return mapToResponse(findRiderById(riderId));
    }

    public RiderResponse getRiderByAuthUserId(String authUserId) {
        requireAuthUserId(authUserId);
        return riderRepository.findByAuthUserId(authUserId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rider profile not found"));
    }

    public RiderResponse updateRider(String riderId, String authUserId, RiderRequest request) {
        Rider rider = findOwnedRider(riderId, authUserId);
        applyRequest(rider, request);
        rider.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(riderRepository.save(rider));
    }

    public void deleteRider(String riderId, String authUserId) {
        riderRepository.delete(findOwnedRider(riderId, authUserId));
    }

    private Rider findOwnedRider(String riderId, String authUserId) {
        Rider rider = findRiderById(riderId);
        requireOwner(rider, authUserId);
        return rider;
    }

    private void requireOwner(Rider rider, String authUserId) {
        requireAuthUserId(authUserId);
        if (!Objects.equals(rider.getAuthUserId(), authUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rider profile does not belong to this account");
        }
    }

    private void requireAuthUserId(String authUserId) {
        if (authUserId == null || authUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }
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
                rider.getAuthUserId(),
                rider.getName(),
                rider.getEmail(),
                rider.getPhone(),
                rider.getCreatedAt(),
                rider.getUpdatedAt()
        );
    }
}
