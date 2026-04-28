package com.example.authsystem.dto;

public record LoginRequest(
        String email,
        String password
) {
}
