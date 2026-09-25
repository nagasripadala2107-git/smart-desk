package com.smartdesk.service;

import com.smartdesk.dto.auth.AuthResponse;
import com.smartdesk.dto.auth.LoginRequest;
import com.smartdesk.dto.auth.RegisterRequest;
import com.smartdesk.entity.Customer;
import com.smartdesk.entity.Profile;
import com.smartdesk.entity.User;
import com.smartdesk.entity.enums.UserRole;
import com.smartdesk.exception.ConflictException;
import com.smartdesk.exception.UnauthorizedException;
import com.smartdesk.repository.*;
import com.smartdesk.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private AuditLogService auditLogService;

    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider tokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);
        tokenProvider = new JwtTokenProvider(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                3600000
        );
        authService = new AuthService(
                userRepository,
                profileRepository,
                customerRepository,
                agentRepository,
                teamRepository,
                passwordEncoder,
                tokenProvider,
                auditLogService
        );
    }

    @Test
    void testRegisterCustomer_Success() {
        RegisterRequest request = new RegisterRequest(
                "john.doe@example.com",
                "Password123!",
                UserRole.CUSTOMER,
                "John",
                "Doe",
                "+1234567890",
                "Acme Corp",
                null,
                null
        );

        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertNotNull(response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals("john.doe@example.com", response.user().email());
        assertEquals(UserRole.CUSTOMER, response.user().role());

        verify(userRepository, times(1)).save(argThat(user ->
                passwordEncoder.matches("Password123!", user.getPasswordHash())
        ));
    }

    @Test
    void testRegister_DuplicateEmail_ThrowsConflictException() {
        RegisterRequest request = new RegisterRequest(
                "existing@example.com",
                "Password123!",
                UserRole.CUSTOMER,
                "Jane",
                "Doe",
                null,
                null,
                null,
                null
        );

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testLogin_Success() {
        String rawPassword = "Password123!";
        String encoded = passwordEncoder.encode(rawPassword);
        User user = new User("alice@example.com", encoded, UserRole.CUSTOMER);
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        LoginRequest loginRequest = new LoginRequest("alice@example.com", rawPassword);
        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertNotNull(response.token());
        assertEquals("alice@example.com", response.user().email());
    }

    @Test
    void testLogin_InvalidPassword_ThrowsUnauthorizedException() {
        String encoded = passwordEncoder.encode("CorrectPassword");
        User user = new User("alice@example.com", encoded, UserRole.CUSTOMER);
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        LoginRequest loginRequest = new LoginRequest("alice@example.com", "WrongPassword");
        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest));
    }
}
