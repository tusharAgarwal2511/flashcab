package com.flashCab.authService.dto;

import com.flashCab.authService.model.UserRole;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant expiresAt,
        String userId,
        String profileId,
        UserRole role) {
}
