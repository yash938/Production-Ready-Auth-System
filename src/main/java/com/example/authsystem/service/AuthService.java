package com.example.authsystem.service;

import com.example.authsystem.dto.UserDto;

public interface AuthService {

    UserDto RegisterUser(UserDto userDto);
}
