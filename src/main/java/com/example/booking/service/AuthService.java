package com.example.booking.service;

import com.example.booking.dto.auth.LoginRequest;
import com.example.booking.dto.auth.LoginResponse;
import com.example.booking.security.JwtService;
import com.example.booking.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        // Delegates to DaoAuthenticationProvider: looks up the user, verifies the
        // BCrypt hash, and throws BadCredentialsException/DisabledException on failure —
        // both mapped to 401 by GlobalExceptionHandler.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        SecurityUser principal = (SecurityUser) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);
        String role = principal.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        return new LoginResponse(token, "Bearer", jwtService.getExpirationMs(), principal.getUsername(), role);
    }
}
