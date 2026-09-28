package com.example.booking.dto.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        String username,
        String role
) {}
