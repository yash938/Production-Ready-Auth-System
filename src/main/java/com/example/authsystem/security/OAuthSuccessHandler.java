package com.example.authsystem.security;

import com.example.authsystem.entity.Provider;
import com.example.authsystem.entity.RefreshToken;
import com.example.authsystem.entity.User;
import com.example.authsystem.repositories.RefreshTokenRepo;
import com.example.authsystem.repositories.UserRepo;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CookieService cookieService;

    @Autowired
    private RefreshTokenRepo refreshTokenRepo;

    @Value("${app.auth.frontend.success-url}")
    private String frontEndUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("Successful Authentication");
        log.info(authentication.toString());

        OAuth2User Oauth2User = (OAuth2User) authentication.getPrincipal();
        //identify user
        String registrationId = "unkonow";
        if (authentication instanceof OAuth2AuthenticationToken token) {
            registrationId = token.getAuthorizedClientRegistrationId();
        }

        log.info("registrationId {}", registrationId);
        log.info("Oauth2User", Oauth2User.getAttributes().toString());

        User user;
        switch (registrationId) {
            case "google" -> {
                String googleId = Oauth2User.getAttributes().getOrDefault("sub", "").toString();

                String email = Oauth2User.getAttributes().getOrDefault("email", "").toString();
                String name = Oauth2User.getAttributes().getOrDefault("name", "").toString();
                String picture = Oauth2User.getAttributes().getOrDefault("picture", "").toString();

                User newUser = User.builder()
                        .providerId(googleId)
                        .name(name)
                        .email(email)
                        .image(picture)
                        .enable(true)
                        .provider(Provider.GOOGLE)
                        .build();


                user = userRepo.findByEmail(email).orElseGet(() -> userRepo.save(newUser));
            }
            case "github" ->{
                String name = Oauth2User.getAttributes().getOrDefault("login","").toString();

                String githubId = Oauth2User.getAttributes().getOrDefault("id","").toString();
                String picture = Oauth2User.getAttributes().getOrDefault("avatar_url","").toString();

                String email = (String) Oauth2User.getAttributes().get("email");

                if(email == null){
                    email = name + "@github.com";
                }
                User newUser = User.builder()
                        .providerId(githubId)
                        .name(name)
                        .email(email)
                        .image(picture)
                        .enable(true)
                        .provider(Provider.GITHUB)
                        .build();

                user = userRepo.findByEmail(email).orElseGet(() -> userRepo.save(newUser));

            }
            default -> {
                throw new RuntimeException("Invalid Registration id");
            }

        }

        //jwt token token ke sath frontend pr fr redirec

        //refresh token banenge
        String jti = UUID.randomUUID().toString();
        RefreshToken refreshTokenOb = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .revoked(false)
                .createdAt(Instant.now())
                .expireAt(Instant.now().plusSeconds(jwtService.getRefreshTokenTime()))
                .build();


        refreshTokenRepo.save(refreshTokenOb);
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, refreshTokenOb.getJti());

        cookieService.attachRefreshCookie(response,refreshToken,(int)jwtService.getRefreshTokenTime());
        response.getWriter().write("login success");
//        response.sendRedirect(frontEndUrl);

    }
}
