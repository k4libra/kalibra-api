package com.kalibra.api.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class JwtCookieFactory {

    public static final String COOKIE_NAME = "token";

    private final long expirationMinutes;
    private final boolean secure;

    // secure=false only for local development over plain http (e.g. the Android emulator hitting
    // 10.0.2.2); every deployed environment keeps the default true.
    public JwtCookieFactory(@Value("${kalibra.jwt.expiration-minutes:30}") long expirationMinutes,
                            @Value("${kalibra.jwt.cookie-secure:true}") boolean secure) {
        this.expirationMinutes = expirationMinutes;
        this.secure = secure;
    }

    public ResponseCookie issue(String token) {
        return build(token, Duration.ofMinutes(expirationMinutes));
    }

    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
