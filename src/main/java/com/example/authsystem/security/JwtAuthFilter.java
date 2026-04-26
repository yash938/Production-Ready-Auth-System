// JwtAuthFilter.java
package com.example.authsystem.security;

import com.example.authsystem.helper.UserIdHelper;
import com.example.authsystem.repositories.UserRepo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepo userRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        log.info("Authorization header : {}",authorization);

        // If no token, continue request
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorization.substring(7);

            // check access token
            if (!jwtService.isAccessToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            Jws<Claims> parsedToken = jwtService.parse(token);

            Claims claims = parsedToken.getPayload();

            String userId = claims.getSubject();
            UUID uuid = UserIdHelper.parseUUID(userId);

            userRepo.findById(uuid).ifPresent(user -> {

                // authenticate only enabled user
                if (user.isEnabled()
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    List<GrantedAuthority> authorities =
                            user.getRoles() == null
                                    ? List.of()
                                    : user.getRoles()
                                      .stream()
                                      .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                                      .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user.getEmail(),
                                    null,
                                    authorities
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                }
            });

        } catch (ExpiredJwtException e) {
            System.out.println("JWT Expired: " + e.getMessage());

        } catch (MalformedJwtException e) {
            System.out.println("Invalid JWT Format: " + e.getMessage());

        } catch (JwtException e) {
            System.out.println("JWT Error: " + e.getMessage());

        } catch (Exception e) {
            System.out.println("General Error: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}