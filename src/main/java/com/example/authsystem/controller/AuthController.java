package com.example.authsystem.controller;

import com.example.authsystem.dto.UserDto;
import com.example.authsystem.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/create")
    public ResponseEntity<UserDto> RegisterUser(@RequestBody UserDto userDto){
        UserDto createUser = authService.RegisterUser(userDto);
        return new ResponseEntity<>(createUser, HttpStatus.CREATED);
    }
}
