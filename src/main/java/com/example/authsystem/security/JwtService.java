package com.example.authsystem.security;

import com.example.authsystem.entity.Role;
import com.example.authsystem.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenTime;
    private final long refreshTokenTime;
    private final String issuer;

    // Constructor Injection
    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-ttl-seconds}") long accessTokenTime,
            @Value("${security.jwt.refresh-ttl-seconds}") long refreshTokenTime,
            @Value("${security.jwt.issuer}") String issuer
    ) {

        if (secret == null || secret.length() < 64) {
            throw new IllegalArgumentException("JWT secret must be at least 64 characters long");
        }

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTime = accessTokenTime;
        this.refreshTokenTime = refreshTokenTime;
        this.issuer = issuer;
    }

    // Generate Access Token
    public String generateToken(User user) {

        Instant now = Instant.now();

        List<String> roles = user.getRoles() == null
                ? List.of()
                : user.getRoles()
                  .stream()
                  .map(role -> role.getRoleName())
                  .toList();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenTime)))
                .claims(Map.of(
                        "email", user.getEmail(),
                        "roles", roles,
                        "typ", "access"
                ))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    // Generate Refresh Token
    public String generateRefreshToken(User user, String jti) {

        Instant now = Instant.now();

        return Jwts.builder()
                .id(jti)
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTokenTime)))
                .claim("typ", "refresh")
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }


    //------------Parse the token-----------------//
    public Jws<Claims> parse(String token){
        try{
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
        }catch (JwtException e){
            throw e;
        }
    }


    public boolean isAccessToken(String token){
        Claims payload = parse(token).getPayload();
        return "access".equals(payload.get("typ"));
    }

    public boolean isAccessRefreshToken(String token){
        Claims payload = parse(token).getPayload();
        return "refresh".equals(payload.get("typ"));
    }


    public UUID getUserIdFromToken(String token){
        Claims payload = parse(token).getPayload();
        return UUID.fromString(payload.getSubject());
    }

    public String getJti(String token){
        return parse(token).getPayload().getId();
    }
}