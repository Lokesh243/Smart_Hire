package com.smarthire.service;

import com.smarthire.dto.request.LoginRequest;
import com.smarthire.dto.request.RegisterRequest;
import com.smarthire.dto.response.AuthResponse;
import com.smarthire.entity.User;
import com.smarthire.enums.Role;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.repository.UserRepository;
import com.smarthire.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .fullName("Test User")
                .email("test@example.com")
                .passwordHash("hashed_password")
                .role(Role.CANDIDATE)
                .isActive(true)
                .build();
    }

    // ── Register tests ─────────────────────────────────────────────

    @Test
    void register_success_returnsAuthResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setRole(Role.CANDIDATE);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtUtil.generateToken(anyString(), any(Role.class), anyLong()))
                .thenReturn("mock_access_token");
        when(jwtUtil.generateRefreshToken(anyString()))
                .thenReturn("mock_refresh_token");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock_access_token");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getRole()).isEqualTo(Role.CANDIDATE);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setRole(Role.CANDIDATE);

        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    // ── Login tests ────────────────────────────────────────────────

    @Test
    void login_success_returnsAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(mockUser));
        when(jwtUtil.generateToken(anyString(), any(Role.class), anyLong()))
                .thenReturn("mock_token");
        when(jwtUtil.generateRefreshToken(anyString()))
                .thenReturn("mock_refresh");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock_token");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void login_userNotFound_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("notfound@example.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any()))
                .thenReturn(null);
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── GetCurrentUser tests ───────────────────────────────────────

    @Test
    void getCurrentUser_existingEmail_returnsUser() {
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(mockUser));

        User result = authService.getCurrentUser("test@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getRole()).isEqualTo(Role.CANDIDATE);
    }

    @Test
    void getCurrentUser_notFound_throwsException() {
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser("ghost@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}