package com.voxflow.security;

import com.voxflow.user.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwt, UserRepository users){this.jwt=jwt;this.users=users;}

    @Override
    protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)
            throws ServletException,IOException {
        String h=req.getHeader("Authorization");
        if(h!=null && h.startsWith("Bearer ")){
            String token=h.substring(7);
            try{
                if(jwt.valid(token)){
                    String email=jwt.extractEmail(token);
                    users.findByEmail(email).ifPresent(u -> {
                        var auth=new UsernamePasswordAuthenticationToken(
                                u.getEmail(),null,List.of(new SimpleGrantedAuthority("ROLE_"+u.getRole())));
                        auth.setDetails(u.getId());
                        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
                    });
                }
            } catch(Exception e) {
                System.err.println("JWT validation failed: " + e.getMessage());
            }
        }
        chain.doFilter(req,res);
    }
}
