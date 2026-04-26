package com.example.authsystem.dto;

import lombok.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponse {

    String message;
    HttpStatus statusCode;
    String error;
    LocalDate timeStamp = LocalDate.now();

    public ErrorResponse(String message, HttpStatus httpStatus, String resourceNotFound) {
    }
}

