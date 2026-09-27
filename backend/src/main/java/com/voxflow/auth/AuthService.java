package com.voxflow.auth;

import com.voxflow.security.JwtService;
import com.voxflow.user.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt;
    public AuthService(UserRepository users,PasswordEncoder encoder,JwtService jwt){this.users=users;this.encoder=encoder;this.jwt=jwt;}

    public AuthResponse register(String email,String password,String name){
        email=email.trim().toLowerCase();
        if(users.existsByEmail(email)) throw new IllegalArgumentException("Email already registered");
        User u=users.save(new User(email,encoder.encode(password),name));
        return new AuthResponse(jwt.generate(u.getId(),u.getEmail()),u.getId(),u.getEmail(),u.getDisplayName());
    }
    public AuthResponse login(String email,String password){
        User u=users.findByEmail(email.trim().toLowerCase()).orElseThrow(()->new IllegalArgumentException("Invalid credentials"));
        if(!encoder.matches(password,u.getPasswordHash())) throw new IllegalArgumentException("Invalid credentials");
        return new AuthResponse(jwt.generate(u.getId(),u.getEmail()),u.getId(),u.getEmail(),u.getDisplayName());
    }
    public record AuthResponse(String token,Long userId,String email,String displayName){}
}
