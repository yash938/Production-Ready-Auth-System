package com.example.authsystem.dto;

import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ApiError(
        HttpStatus status,
        String message,
        String error,
        String path,
        OffsetDateTime timeStamp
) {
    public static ApiError of(HttpStatus status, String message,String error,String path){
        return new ApiError(status,message,error,path,OffsetDateTime.now(ZoneOffset.UTC));
    }

}
