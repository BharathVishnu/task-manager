package taskamanager.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import taskamanager.backend.exception.AppException;
import taskamanager.backend.model.RefreshToken;
import taskamanager.backend.repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {
    public record Rotated(Long userId, String newRawToken) {}

    private static final SecureRandom RANDOM = new SecureRandom();
    private final RefreshTokenRepository repo;
    private final Duration ttl;

    public RefreshTokenService(RefreshTokenRepository repo, AppProperties props) {
        this.repo = repo;
        this.ttl = props.refresh().ttl();
    }

    /** New login session = new family. Returns the RAW token (only its hash is stored). */
    @Transactional
    public String issueNewFamily(Long userId) {
        return issue(userId, UUID.randomUUID());
    }

    private String issue(Long userId, UUID familyId) {
        String raw = randomToken();
        repo.save(new RefreshToken(userId, familyId, sha256(raw), Instant.now().plus(ttl)));
        return raw;
    }

    /**
     * noRollbackFor: in the reuse case we revoke the family and THEN throw.
     * Without this, the thrown exception would roll back the revocation.
     */
    @Transactional(noRollbackFor = AppException.class)
    public Rotated rotate(String raw) {
        RefreshToken old = repo.findByTokenHash(sha256(raw))
                .orElseThrow(() -> invalid());

        if (old.getRevokedAt() != null) {                 // REUSE DETECTED: assume theft
            repo.revokeFamily(old.getFamilyId(), Instant.now());
            throw invalid();
        }
        if (old.getExpiresAt().isBefore(Instant.now())) throw invalid();

        // Atomic claim: if two requests race with the same token, only one gets 1 here
        if (repo.revokeIfActive(old.getId(), Instant.now()) == 0) throw invalid();

        String newRaw = issue(old.getUserId(), old.getFamilyId());   // same family
        return new Rotated(old.getUserId(), newRaw);
    }

    /** Logout: kill the whole session family. Idempotent, never throws for unknown tokens. */
    @Transactional
    public void revokeByRawToken(String raw) {
        if (raw == null || raw.isBlank()) return;
        repo.findByTokenHash(sha256(raw))
            .ifPresent(t -> repo.revokeFamily(t.getFamilyId(), Instant.now()));
    }

    private static AppException invalid() {
        return new AppException(401, "REFRESH_INVALID", "Please log in again");
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];                       // 256 bits of randomness
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 is right here (not BCrypt): the token is already high-entropy random. */
    static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
