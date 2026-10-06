package taskamanager.backend.controller;


import taskamanager.backend.dto.Dtos.UserResponse;
import taskamanager.backend.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {
    private final AuthService auth;
    public UserController(AuthService auth) { this.auth = auth; }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Long userId) { return auth.me(userId); }
}