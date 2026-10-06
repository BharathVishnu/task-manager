package taskamanager.backend.security;


import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import taskamanager.backend.config.AppProperties;

@Component
public class RefreshCookieFactory {
    public static final String NAME = "refresh_token";
    private final AppProperties.Refresh cfg;

    public RefreshCookieFactory(AppProperties props) { this.cfg = props.refresh(); }

    public ResponseCookie build(String rawToken) { return base(rawToken, cfg.ttl()); }

    /** Expired cookie with the same attributes, so the browser deletes it. */
    public ResponseCookie clear() { return base("", Duration.ZERO); }

    private ResponseCookie base(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)                    // JavaScript cannot read it (XSS protection)
                .secure(cfg.cookieSecure())        // true over HTTPS in production
                .sameSite(cfg.cookieSameSite())    // CSRF protection
                .path("/auth")                     // browser sends it ONLY to /auth/*
                .maxAge(maxAge)
                .build();
    }
}