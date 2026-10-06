package taskamanager.backend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed binding of the "app:" section of application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Refresh refresh, Cors cors) {
    public record Jwt(String secret, Duration accessTtl) {}
    public record Refresh(Duration ttl, boolean cookieSecure, String cookieSameSite) {}
    public record Cors(String allowedOrigin) {}
}
