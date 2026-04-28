package com.example.authsystem.controller;

import com.example.authsystem.dto.LoginRequest;
import com.example.authsystem.dto.TokenResponse;
import com.example.authsystem.dto.UserDto;
import com.example.authsystem.entity.RefreshToken;
import com.example.authsystem.entity.User;
import com.example.authsystem.repositories.RefreshTokenRepo;
import com.example.authsystem.repositories.UserRepo;
import com.example.authsystem.security.JwtService;
import com.example.authsystem.service.AuthService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private RefreshTokenRepo refreshTokenRepo;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ModelMapper modelMapper;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody LoginRequest loginRequest
    ){
        Authentication authenticate = authenticate(loginRequest);
        User user = userRepo.findByEmail(loginRequest.email()).orElseThrow(() -> new BadCredentialsException("Invalid Username or Password"));

        if(!user.isEnabled()){
            throw new DisabledException("User is disabled");
        }

        String jti = UUID.randomUUID().toString();
        RefreshToken refToken = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expireAt(Instant.now().plusSeconds(jwtService.getRefreshTokenTime()))
                .revoked(false)
                .build();

        refreshTokenRepo.save(refToken);

        //generate token
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, refToken.getJti());
        TokenResponse token = TokenResponse.bearer(accessToken, refreshToken, jwtService.getAccessTokenTime(), modelMapper.map(user, UserDto.class));
        return new ResponseEntity<>(token,HttpStatus.CREATED);
    }

    private Authentication authenticate(LoginRequest loginRequest) {
        try {
           return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.email(),loginRequest.password()));
        }catch (Exception e){
            throw new BadCredentialsException("Invalid Credentials "+e.getMessage());
        }
    }


    @PostMapping("/register")
    public ResponseEntity<UserDto> RegisterUser(@RequestBody UserDto userDto){
        UserDto createUser = authService.RegisterUser(userDto);
        return new ResponseEntity<>(createUser, HttpStatus.CREATED);
    }
}
