package taskamanager.backend.controller;

import taskamanager.backend.dto.Dtos.*;
import taskamanager.backend.exception.AppException;
import taskamanager.backend.security.RefreshCookieFactory;
import taskamanager.backend.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService auth;
    private final RefreshCookieFactory cookies;

    public AuthController(AuthService auth, RefreshCookieFactory cookies) {
        this.auth = auth; this.cookies = cookies;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest req) {
        return auth.signup(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req, HttpServletResponse res) {
        AuthService.Tokens t = auth.login(req);
        res.addHeader(HttpHeaders.SET_COOKIE, cookies.build(t.refreshToken()).toString());
        return new AuthResponse(t.accessToken(), t.expiresInSeconds());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @CookieValue(name = RefreshCookieFactory.NAME, required = false) String raw,
            HttpServletResponse res) {
        if (raw == null) throw new AppException(401, "REFRESH_INVALID", "Please log in again");
        AuthService.Tokens t = auth.refresh(raw);
        res.addHeader(HttpHeaders.SET_COOKIE, cookies.build(t.refreshToken()).toString());   // rotated
        return new AuthResponse(t.accessToken(), t.expiresInSeconds());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CookieValue(name = RefreshCookieFactory.NAME, required = false) String raw,
            HttpServletResponse res) {
        auth.logout(raw);
        res.addHeader(HttpHeaders.SET_COOKIE, cookies.clear().toString());
    }
} 

