package com.example.authsystem.service;

import com.example.authsystem.dto.UserDto;
import com.example.authsystem.entity.User;

public interface UserService {

    UserDto createUser(UserDto userDto);

    UserDto updateUser(String userId, UserDto userDto);

    void deleteUser(String userId);

    UserDto getUserId(String userId);

    Iterable<UserDto> getAllUser();

    UserDto findByEmail(String email);

}
