package com.transport.userservice.service;

import com.transport.userservice.dto.AuthResponse;
import com.transport.userservice.dto.LoginRequest;
import com.transport.userservice.dto.RegisterRequest;
import com.transport.userservice.dto.UserResponse;
import com.transport.userservice.entity.User;
import com.transport.userservice.repository.UserRepository;
import com.transport.userservice.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService
 * Tests all user-related operations: registration, login, and profile retrieval
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User mockUser;
    private String encodedPassword;

    @BeforeEach
    void setUp() {
        encodedPassword = "$2a$10$encodedPasswordHash";
        
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setPhoneNumber("1234567890");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        mockUser = User.builder()
                .id("user-id-123")
                .email("test@example.com")
                .password(encodedPassword)
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .role(User.UserRole.USER)
                .build();
    }

    // ==================== Registration Tests ====================

    @Test
    @DisplayName("Should successfully register a new user")
    void whenRegister_withValidRequest_thenReturnAuthResponse() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn(encodedPassword);
        // Simulate JPA setting the ID after save
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-id-123");
            return user;
        });
        when(jwtUtil.generateToken(anyString(), any())).thenReturn("jwt-token-123");

        // When
        AuthResponse result = userService.register(registerRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("jwt-token-123");
        assertThat(result.getUserId()).isEqualTo("user-id-123");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");

        verify(userRepository, times(1)).existsByEmail(registerRequest.getEmail());
        verify(passwordEncoder, times(1)).encode(registerRequest.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtUtil, times(1)).generateToken(anyString(), any());
    }

    @Test
    @DisplayName("Should encode password before saving user")
    void whenRegister_thenPasswordIsEncoded() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-id-123");
            return user;
        });
        when(jwtUtil.generateToken(anyString(), any())).thenReturn("jwt-token-123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // When
        userService.register(registerRequest);

        // Then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
        assertThat(savedUser.getPassword()).isNotEqualTo(registerRequest.getPassword());
    }

    @Test
    @DisplayName("Should set role to USER when registering")
    void whenRegister_thenRoleIsSetToUser() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-id-123");
            return user;
        });
        when(jwtUtil.generateToken(anyString(), any())).thenReturn("jwt-token-123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // When
        userService.register(registerRequest);

        // Then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(User.UserRole.USER);
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void whenRegister_withExistingEmail_thenThrowException() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, times(1)).existsByEmail(registerRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(jwtUtil, never()).generateToken(anyString(), any());
    }

    @Test
    @DisplayName("Should save user with all provided information")
    void whenRegister_thenSaveUserWithAllFields() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-id-123");
            return user;
        });
        when(jwtUtil.generateToken(anyString(), any())).thenReturn("jwt-token-123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // When
        userService.register(registerRequest);

        // Then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(registerRequest.getEmail());
        assertThat(savedUser.getFirstName()).isEqualTo(registerRequest.getFirstName());
        assertThat(savedUser.getLastName()).isEqualTo(registerRequest.getLastName());
        assertThat(savedUser.getPhoneNumber()).isEqualTo(registerRequest.getPhoneNumber());
    }

    // ==================== Login Tests ====================

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void whenLogin_withValidCredentials_thenReturnAuthResponse() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), encodedPassword)).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), any())).thenReturn("jwt-token-123");

        // When
        AuthResponse result = userService.login(loginRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("jwt-token-123");
        assertThat(result.getUserId()).isEqualTo("user-id-123");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");

        verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, times(1)).matches(loginRequest.getPassword(), encodedPassword);
        verify(jwtUtil, times(1)).generateToken(anyString(), any());
    }

    @Test
    @DisplayName("Should throw exception when user not found during login")
    void whenLogin_withNonExistentEmail_thenThrowException() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");

        verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString(), any());
    }

    @Test
    @DisplayName("Should throw exception when password is incorrect")
    void whenLogin_withIncorrectPassword_thenThrowException() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), encodedPassword)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");

        verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, times(1)).matches(loginRequest.getPassword(), encodedPassword);
        verify(jwtUtil, never()).generateToken(anyString(), any());
    }

    @Test
    @DisplayName("Should verify password using password encoder")
    void whenLogin_thenVerifyPasswordWithEncoder() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), encodedPassword)).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), any())).thenReturn("jwt-token-123");

        // When
        userService.login(loginRequest);

        // Then
        verify(passwordEncoder, times(1)).matches(loginRequest.getPassword(), encodedPassword);
    }

    // ==================== Get Profile Tests ====================

    @Test
    @DisplayName("Should successfully retrieve user profile")
    void whenGetProfile_withValidUserId_thenReturnUserResponse() {
        // Given
        String userId = "user-id-123";
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // When
        UserResponse result = userService.getProfile(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("user-id-123");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getPhoneNumber()).isEqualTo("1234567890");

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void whenGetProfile_withInvalidUserId_thenThrowException() {
        // Given
        String userId = "non-existent-id";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getProfile(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Should return user response with all user details")
    void whenGetProfile_thenReturnAllUserDetails() {
        // Given
        User userWithAllFields = User.builder()
                .id("user-id-456")
                .email("another@example.com")
                .password(encodedPassword)
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("9876543210")
                .role(User.UserRole.USER)
                .build();

        when(userRepository.findById("user-id-456")).thenReturn(Optional.of(userWithAllFields));

        // When
        UserResponse result = userService.getProfile("user-id-456");

        // Then
        assertThat(result.getId()).isEqualTo("user-id-456");
        assertThat(result.getEmail()).isEqualTo("another@example.com");
        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getLastName()).isEqualTo("Smith");
        assertThat(result.getPhoneNumber()).isEqualTo("9876543210");
    }

    // ==================== Build Auth Response Tests ====================

    @Test
    @DisplayName("Should include userId in JWT claims")
    @SuppressWarnings("unchecked")
    void whenBuildAuthResponse_thenIncludeUserIdInClaims() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-id-123");
            return user;
        });
        
        ArgumentCaptor<Map> claimsCaptor = ArgumentCaptor.forClass(Map.class);
        when(jwtUtil.generateToken(anyString(), claimsCaptor.capture())).thenReturn("jwt-token-123");

        // When
        userService.register(registerRequest);

        // Then
        Map<String, Object> claims = (Map<String, Object>) claimsCaptor.getValue();
        assertThat(claims).isNotNull();
        assertThat(claims.get("userId")).isEqualTo("user-id-123");
    }

    @Test
    @DisplayName("Should generate token with user email as subject")
    void whenBuildAuthResponse_thenUseEmailAsTokenSubject() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-id-123");
            return user;
        });
        
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        when(jwtUtil.generateToken(emailCaptor.capture(), any())).thenReturn("jwt-token-123");

        // When
        userService.register(registerRequest);

        // Then
        assertThat(emailCaptor.getValue()).isEqualTo("test@example.com");
    }
}
