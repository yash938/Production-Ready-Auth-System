package com.example.authsystem.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        UserDto userDto
) {
    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn, UserDto userDto) {
     return new TokenResponse(accessToken,refreshToken,expiresIn,"Bearer",userDto);
    }
}
