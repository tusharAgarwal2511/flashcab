package com.flashCab.authService.dto;

import com.flashCab.authService.model.UserRole;

public record SignupResponse(
        String userId,
        String profileId,
        String email,
        UserRole role) {
}
