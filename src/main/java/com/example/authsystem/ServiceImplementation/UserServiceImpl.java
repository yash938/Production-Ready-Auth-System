package com.example.authsystem.ServiceImplementation;

import com.example.authsystem.dto.UserDto;
import com.example.authsystem.entity.Provider;
import com.example.authsystem.entity.User;
import com.example.authsystem.exceptions.ResourceNotFoundException;
import com.example.authsystem.helper.UserIdHelper;
import com.example.authsystem.repositories.UserRepo;
import com.example.authsystem.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public UserDto createUser(UserDto userDto) {

        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email already Exists");
        }

        if (userRepo.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = modelMapper.map(userDto, User.class);
        user.setProvider(userDto.getProvider() != null ? userDto.getProvider() : Provider.LOCAL);
        User savedUser = userRepo.save(user);

        return modelMapper.map(savedUser, UserDto.class);
    }

    @Override
    public UserDto updateUser(String userId, UserDto userDto) {
        UUID uuid = UserIdHelper.parseUUID(userId);
        User users = userRepo.findById(uuid).orElseThrow(() -> new ResourceNotFoundException("User is not found"));
        users.setName(userDto.getName());
        users.setPassword(userDto.getPassword());
        users.setEmail(userDto.getEmail());
        users.setImage(userDto.getImage());
        users.setUpdatedAt(Instant.now());
        User savedUser = userRepo.save(users);
        return modelMapper.map(savedUser,UserDto.class);
    }


    @Override
    public void deleteUser(String userId) {
        UUID uuid = UserIdHelper.parseUUID(userId);
        User user = userRepo.findById(uuid).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepo.delete(user);
        System.out.println("User is deleted "+user);
    }

    @Override
    public UserDto getUserId(String userId) {
        UUID uuid = UserIdHelper.parseUUID(userId);
        User user = userRepo.findById(uuid).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return modelMapper.map(user,UserDto.class);
    }

    @Override
    public Iterable<UserDto> getAllUser() {

        return userRepo.findAll()
                .stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
    }

    @Override
    public UserDto findByEmail(String email) {
        User user = userRepo.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return modelMapper.map(user,UserDto.class);
    }
}
