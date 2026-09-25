package com.smartdesk.service;

import com.smartdesk.dto.auth.UserResponse;
import com.smartdesk.entity.User;
import com.smartdesk.exception.ResourceNotFoundException;
import com.smartdesk.mapper.EntityDtoMapper;
import com.smartdesk.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getUserEntity(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return EntityDtoMapper.toUserResponse(getUserEntity(id));
    }
}
