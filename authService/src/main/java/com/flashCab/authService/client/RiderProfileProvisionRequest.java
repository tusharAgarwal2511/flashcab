package com.flashCab.authService.client;

public record RiderProfileProvisionRequest(
        String authUserId,
        String name,
        String email,
        String phone) {
}
