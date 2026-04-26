package com.example.authsystem.controller;

import com.example.authsystem.dto.DeleteUsers;
import com.example.authsystem.dto.UserDto;
import com.example.authsystem.entity.User;
import com.example.authsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/create")
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto){
        UserDto user = userService.createUser(userDto);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> updateUser(@PathVariable("userId") String userId, @RequestBody UserDto userDto){
        UserDto updatedUser = userService.updateUser(userId, userDto);
        return new ResponseEntity<>(updatedUser,HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<Iterable<UserDto>> getAllUsers(){
        Iterable<UserDto> allUser = userService.getAllUser();
        return new ResponseEntity<>(allUser,HttpStatus.OK);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<DeleteUsers> deleteUser(@PathVariable("userId") String userId){
        userService.deleteUser(userId);
        DeleteUsers deleteUsers = new DeleteUsers("User is deleted",HttpStatus.OK);
        return new ResponseEntity<>(deleteUsers,HttpStatus.OK);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserByID(@PathVariable String userId){
        UserDto findId = userService.getUserId(userId);
        return new ResponseEntity<>(findId,HttpStatus.OK);
    }




}
