package com.example.authsystem.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@Getter
@Slf4j
public class CookieService {

    private final String refreshTokenCookieName;
    private final boolean cookieHttpOnly;
    private final boolean cookieSecure;
    private final String cookieDomain;
    private final String cookieSameSite;

    public CookieService(
            @Value("${security.jwt.refreshtoken-cookie-name}") String refreshTokenCookieName,
            @Value("${security.jwt.cookie-hhtp-only}") boolean cookieHttpOnly,
            @Value("${security.jwt.cookie-secure}") boolean cookieSecure,
            @Value("${security.jwt.cookie-domain}") String cookieDomain,
            @Value("${security.jwt.cookie-same-site}") String cookieSameSite
    ) {
        this.refreshTokenCookieName = refreshTokenCookieName;
        this.cookieHttpOnly = cookieHttpOnly;
        this.cookieSecure = cookieSecure;
        this.cookieDomain = cookieDomain;
        this.cookieSameSite = cookieSameSite;
    }

    // ATTACH COOKIE
    public void attachRefreshCookie(HttpServletResponse response, String value, int maxAge) {

        log.info("🔹 Attaching refresh cookie...");
        log.info("Cookie Name: {}", refreshTokenCookieName);
        log.info("Token Value (short): {}...", value.substring(0, Math.min(15, value.length())));
        log.info("Max Age: {}", maxAge);
        log.info("HttpOnly: {}, Secure: {}, SameSite: {}", cookieHttpOnly, cookieSecure, cookieSameSite);

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshTokenCookieName, value)
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAge)
                .sameSite(cookieSameSite);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
            log.info("Domain set: {}", cookieDomain);
        }

        ResponseCookie cookie = builder.build();

        log.info("Final Set-Cookie Header: {}", cookie.toString());

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    //CLEAR COOKIE
    public void clearRefreshCookie(HttpServletResponse response) {

        log.info("🔹 Clearing refresh cookie: {}", refreshTokenCookieName);

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshTokenCookieName, "")
                .maxAge(0)
                .httpOnly(cookieHttpOnly)
                .path("/")
                .sameSite(cookieSameSite)
                .secure(cookieSecure);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        ResponseCookie cookie = builder.build();

        log.info("Clear Cookie Header: {}", cookie.toString());

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // CACHE CONTROL
    public void addNoStoreHeader(HttpServletResponse response) {
        log.info("Adding no-store headers");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("pragma", "no-cache");
    }
}