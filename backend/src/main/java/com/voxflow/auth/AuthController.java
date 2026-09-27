package com.voxflow.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth){this.auth=auth;}

    @PostMapping("/register") public AuthService.AuthResponse register(@Valid @RequestBody RegisterRequest r){
        return auth.register(r.email(),r.password(),r.displayName());
    }
    @PostMapping("/login") public AuthService.AuthResponse login(@Valid @RequestBody LoginRequest r){
        return auth.login(r.email(),r.password());
    }
    public record RegisterRequest(@Email @NotBlank String email,@Size(min=8) String password,@NotBlank String displayName){}
    public record LoginRequest(@Email @NotBlank String email,@NotBlank String password){}
}
