package com.example.authsystem.controller;

import com.example.authsystem.dto.*;
import com.example.authsystem.entity.RefreshToken;
import com.example.authsystem.entity.User;
import com.example.authsystem.repositories.RefreshTokenRepo;
import com.example.authsystem.repositories.UserRepo;
import com.example.authsystem.security.CookieService;
import com.example.authsystem.security.JwtService;
import com.example.authsystem.service.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AuthService authService;
    @Autowired private RefreshTokenRepo refreshTokenRepo;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepo userRepo;
    @Autowired private ModelMapper modelMapper;
    @Autowired private CookieService cookieService;

    // ================= LOGIN =================
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletResponse response
    ) {

        authenticate(loginRequest);

        User user = userRepo.findByEmail(loginRequest.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid Username or Password"));

        if (!user.isEnabled()) {
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

        String accessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user, jti);

        cookieService.attachRefreshCookie(response, newRefreshToken, (int) jwtService.getRefreshTokenTime());
        cookieService.addNoStoreHeader(response);

        System.out.println("LOGIN SUCCESS");

        return new ResponseEntity<>(
                TokenResponse.bearer(accessToken, newRefreshToken,
                        jwtService.getAccessTokenTime(),
                        modelMapper.map(user, UserDto.class)),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       HttpServletResponse response) {
        readRefreshTokenFromRequest(null, request).ifPresent(token -> {
            try {
                if (jwtService.isRefreshToken(token)) {

                    String jti = jwtService.getJti(token);

                    refreshTokenRepo.findByJti(jti).ifPresent(rt -> {
                        rt.setRevoked(true);
                        refreshTokenRepo.save(rt);
                        System.out.println("Refresh token revoked: " + jti);
                    });
                }
            } catch (JwtException jwt) {
                System.out.println("Invalid token during logout: " + jwt.getMessage());
                //logout me exception throw nahi karte (user already logout ho raha hai)
            }
        });

        //IMPORTANT: cookie delete karo
        cookieService.clearRefreshCookie(response);
        cookieService.addNoStoreHeader(response);
        SecurityContextHolder.clearContext();

        System.out.println("Logout successful");

        return ResponseEntity.noContent().build(); // 204
    }

    // ================= REFRESH =================
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletResponse response,
            HttpServletRequest request
    ) {

        System.out.println("\n=====REFRESH API =====");

        String refreshToken = readRefreshTokenFromRequest(body, request)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        System.out.println("Token received");

        //Parse
        var claims = jwtService.parse(refreshToken).getPayload();

        System.out.println("TYPE: " + claims.get("typ"));
        System.out.println("JTI: " + claims.getId());

        //Type check
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token type");
        }

        String jti = claims.getId();
        UUID userIdFromToken = UUID.fromString(claims.getSubject());

        RefreshToken storedToken = refreshTokenRepo.findByJti(jti)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        // Validations
        if (storedToken.isRevoked()) {
            throw new BadCredentialsException("Token revoked");
        }

        if (storedToken.getExpireAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Token expired");
        }

        if (!storedToken.getUser().getId().equals(userIdFromToken)) {
            throw new BadCredentialsException("User mismatch");
        }

        System.out.println("TOKEN VALID");

        // ROTATE TOKEN
        storedToken.setRevoked(true);
        String newJti = UUID.randomUUID().toString();
        storedToken.setReplaceByToken(newJti);
        refreshTokenRepo.save(storedToken);

        User user = storedToken.getUser();

        RefreshToken newToken = RefreshToken.builder()
                .jti(newJti)
                .user(user)
                .createdAt(Instant.now())
                .expireAt(Instant.now().plusSeconds(jwtService.getRefreshTokenTime()))
                .revoked(false)
                .build();

        refreshTokenRepo.save(newToken);

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user, newJti);

        cookieService.attachRefreshCookie(response, newRefreshToken,
                (int) jwtService.getRefreshTokenTime());
        cookieService.addNoStoreHeader(response);

        System.out.println("NEW TOKENS GENERATED");

        return ResponseEntity.ok(
                TokenResponse.bearer(newAccessToken, newRefreshToken,
                        jwtService.getAccessTokenTime(),
                        modelMapper.map(user, UserDto.class))
        );
    }

    // ================= TOKEN READER =================
    private Optional<String> readRefreshTokenFromRequest(
            RefreshTokenRequest body,
            HttpServletRequest request
    ) {

        //1. BODY FIRST (FIXED)
        if (body != null && body.refreshToken() != null && !body.refreshToken().isBlank()) {
            System.out.println("Token from BODY");
            return Optional.of(body.refreshToken());
        }

        // 2. COOKIE
        if (request.getCookies() != null) {
            Optional<String> fromCookie = Arrays.stream(request.getCookies())
                    .filter(c -> cookieService.getRefreshTokenCookieName().equals(c.getName()))
                    .map(Cookie::getValue)
                    .filter(v -> !v.isBlank())
                    .findFirst();

            if (fromCookie.isPresent()) {
                System.out.println("Token from COOKIE");
                return fromCookie;
            }
        }

        // 3. HEADER
        String header = request.getHeader("X-Refresh-Token");
        if (header != null && !header.isBlank()) {
            System.out.println("Token from HEADER");
            return Optional.of(header);
        }

        System.out.println("No token found");
        return Optional.empty();
    }

    // ================= REGISTER =================
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody UserDto userDto) {
        return new ResponseEntity<>(authService.RegisterUser(userDto), HttpStatus.CREATED);
    }

    // ================= AUTH =================
    private Authentication authenticate(LoginRequest loginRequest) {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email(),
                            loginRequest.password()
                    )
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid Credentials");
        }
    }
}