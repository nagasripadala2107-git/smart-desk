package com.smartdesk.controller;

import com.smartdesk.dto.auth.AuthResponse;
import com.smartdesk.dto.auth.LoginRequest;
import com.smartdesk.dto.auth.RegisterRequest;
import com.smartdesk.dto.auth.UserResponse;
import com.smartdesk.dto.common.ApiResponse;
import com.smartdesk.entity.User;
import com.smartdesk.security.UserPrincipal;
import com.smartdesk.service.AuthService;
import com.smartdesk.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(ApiResponse.ok("Registration successful", response), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new com.smartdesk.exception.UnauthorizedException("Authentication required to access current user profile");
        }
        UserResponse user = userService.getUserById(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.ok(user));
    }
}
