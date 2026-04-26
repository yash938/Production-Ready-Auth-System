package com.example.authsystem.ServiceImplementation;

import com.example.authsystem.dto.UserDto;
import com.example.authsystem.entity.Provider;
import com.example.authsystem.entity.User;
import com.example.authsystem.repositories.UserRepo;
import com.example.authsystem.service.AuthService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepo userRepo;
    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDto RegisterUser(UserDto userDto) {
        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email already Exists");
        }

        if (userRepo.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        User users = modelMapper.map(userDto, User.class);
        users.setProvider(userDto.getProvider() != null ? userDto.getProvider() : Provider.LOCAL);
        users.setPassword(passwordEncoder.encode(userDto.getPassword()));
        User createUser = userRepo.save(users);
        return modelMapper.map(createUser, UserDto.class);
    }
}
