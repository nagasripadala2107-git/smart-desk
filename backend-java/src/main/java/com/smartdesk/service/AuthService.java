package com.smartdesk.service;

import com.smartdesk.dto.auth.AuthResponse;
import com.smartdesk.dto.auth.LoginRequest;
import com.smartdesk.dto.auth.RegisterRequest;
import com.smartdesk.dto.auth.UserResponse;
import com.smartdesk.entity.*;
import com.smartdesk.entity.enums.AgentAvailability;
import com.smartdesk.entity.enums.UserRole;
import com.smartdesk.exception.BadRequestException;
import com.smartdesk.exception.ConflictException;
import com.smartdesk.exception.UnauthorizedException;
import com.smartdesk.mapper.EntityDtoMapper;
import com.smartdesk.repository.*;
import com.smartdesk.security.JwtTokenProvider;
import com.smartdesk.security.UserPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final CustomerRepository customerRepository;
    private final AgentRepository agentRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuditLogService auditLogService;

    public AuthService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            CustomerRepository customerRepository,
            AgentRepository agentRepository,
            TeamRepository teamRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.customerRepository = customerRepository;
        this.agentRepository = agentRepository;
        this.teamRepository = teamRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() != UserRole.CUSTOMER) {
            throw new BadRequestException("Public registration is only permitted for CUSTOMER accounts. Admin and Agent accounts cannot be registered publicly.");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email is already registered: " + request.email());
        }

        // 1. Create and save User with hashed password
        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.role()
        );
        user = userRepository.save(user);

        // 2. Create and save Profile
        Profile profile = new Profile(
                user,
                request.firstName(),
                request.lastName(),
                request.phone(),
                null
        );
        profileRepository.save(profile);
        user.setProfile(profile);

        // 3. Create role-specific entities
        if (request.role() == UserRole.CUSTOMER) {
            String customerCode = "CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Customer customer = new Customer(
                    user,
                    customerCode,
                    request.companyName() != null ? request.companyName() : request.firstName() + " Organization",
                    "STANDARD"
            );
            customerRepository.save(customer);
            user.setCustomer(customer);
        } else if (request.role() == UserRole.AGENT) {
            String employeeCode = request.employeeCode() != null
                    ? request.employeeCode()
                    : "EMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            Team team = null;
            if (request.teamName() != null) {
                team = teamRepository.findByName(request.teamName()).orElse(null);
            }

            Agent agent = new Agent(
                    user,
                    team,
                    employeeCode,
                    AgentAvailability.OFFLINE,
                    "general",
                    5
            );
            agentRepository.save(agent);
            user.setAgent(agent);
        }

        auditLogService.logAction(user, "USER_REGISTERED", "User", user.getId().toString(), null, "{\"email\":\"" + user.getEmail() + "\"}", null, null);

        // 4. Generate JWT
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String token = tokenProvider.generateToken(userPrincipal);
        UserResponse userResponse = EntityDtoMapper.toUserResponse(user);

        return new AuthResponse(token, tokenProvider.getExpirationMs(), userResponse);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String token = tokenProvider.generateToken(userPrincipal);
        UserResponse userResponse = EntityDtoMapper.toUserResponse(user);

        auditLogService.logAction(user, "USER_LOGGED_IN", "User", user.getId().toString(), null, null, null, null);

        return new AuthResponse(token, tokenProvider.getExpirationMs(), userResponse);
    }
}
