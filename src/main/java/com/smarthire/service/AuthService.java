package com.smarthire.service;

import com.smarthire.dto.request.LoginRequest;
import com.smarthire.dto.request.RegisterRequest;
import com.smarthire.dto.response.AuthResponse;
import com.smarthire.entity.User;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.exception.UnauthorizedException;
import com.smarthire.repository.UserRepository;
import com.smarthire.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    // ── Register ───────────────────────────────────────────────────
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: "
                    + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {} with role: {}",
                savedUser.getEmail(), savedUser.getRole());

        String accessToken = jwtUtil.generateToken(
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getId());

        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }


    // ── Login ──────────────────────────────────────────────────────
    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + request.getEmail()));

        String accessToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole(),
                user.getId());

        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    // ── Get current user ───────────────────────────────────────────
    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + email));
    }
    // ── Refresh Token ──────────────────────────────────────────────
public AuthResponse refreshToken(String refreshToken) {

    // 1. extract email (also verifies signature internally)
    String email;
    try {
        email = jwtUtil.extractEmail(refreshToken);
    } catch (Exception e) {
        throw new UnauthorizedException("Invalid refresh token");
    }

    // 2. check not expired + email matches
    if (!jwtUtil.isTokenValid(refreshToken, email)) {
        throw new UnauthorizedException("Refresh token expired or invalid");
    }

    // 3. load user from DB
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

    // 4. issue a brand new access token
    String newAccessToken = jwtUtil.generateToken(
            user.getEmail(),
            user.getRole(),
            user.getId());

    log.info("Access token refreshed for user: {}", email);

    return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)   // same refresh token, not rotated
            .tokenType("Bearer")
            .userId(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .role(user.getRole())
            .build();
}

}