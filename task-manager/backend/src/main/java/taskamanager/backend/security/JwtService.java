package taskamanager.backend.security;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import taskamanager.backend.config.AppProperties;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(AppProperties props) {
        this.key = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = props.jwt().accessTtl().toSeconds();
    }

    public long ttlSeconds() { return ttlSeconds; }

    /** Claims: sub = user id, iat, exp. No role inside: roles are per project and can change. */
    public String createAccessToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    /** Throws ExpiredJwtException / JwtException if expired, tampered, or malformed. */
    public Long parseUserId(String token) {
        String sub = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject();
        return Long.valueOf(sub);
    }
}
