package taskamanager.backend.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import taskamanager.backend.dto.Dtos.LoginRequest;
import taskamanager.backend.dto.Dtos.SignupRequest;
import taskamanager.backend.dto.Dtos.UserResponse;
import taskamanager.backend.exception.AppException;
import taskamanager.backend.model.User;
import taskamanager.backend.repository.UserRepository;
import taskamanager.backend.security.JwtService;

@Service
public class AuthService {
    public record Tokens(String accessToken, long expiresInSeconds, String refreshToken) {}

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;
    private final String dummyHash;   

    public AuthService(UserRepository users, PasswordEncoder encoder,
                       JwtService jwt, RefreshTokenService refreshTokens) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
        this.dummyHash = encoder.encode("dummy-password-for-timing");
    }

    public UserResponse signup(SignupRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw emailTaken();
        try {
            User u = users.save(new User(email, encoder.encode(req.password())));
            return new UserResponse(u.getId(), u.getEmail());
        } catch (DataIntegrityViolationException e) {
            throw emailTaken();   // the unique index is the real guard against a race
        }
    }

    public Tokens login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email().trim()).orElse(null);
        // Always run one BCrypt compare so timing doesn't reveal whether the email exists
        boolean ok = encoder.matches(req.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !ok) throw new AppException(401, "BAD_LOGIN", "Wrong email or password");

        String refresh = refreshTokens.issueNewFamily(user.getId());
        return new Tokens(jwt.createAccessToken(user.getId()), jwt.ttlSeconds(), refresh);
    }

    public Tokens refresh(String rawRefreshToken) {
        var rotated = refreshTokens.rotate(rawRefreshToken);
        return new Tokens(jwt.createAccessToken(rotated.userId()), jwt.ttlSeconds(), rotated.newRawToken());
    }

    public void logout(String rawRefreshToken) {
        refreshTokens.revokeByRawToken(rawRefreshToken);
    }

    public UserResponse me(Long userId) {
        User u = users.findById(userId).orElseThrow(AppException::notFound);
        return new UserResponse(u.getId(), u.getEmail());
    }

    private static AppException emailTaken() {
        return new AppException(409, "EMAIL_TAKEN", "Email already registered");
    }
}
