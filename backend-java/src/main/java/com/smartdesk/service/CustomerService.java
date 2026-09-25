package com.smartdesk.service;

import com.smartdesk.dto.customer.CustomerProfileResponse;
import com.smartdesk.dto.customer.CustomerResponse;
import com.smartdesk.entity.Customer;
import com.smartdesk.entity.User;
import com.smartdesk.entity.enums.TicketStatus;
import com.smartdesk.exception.ResourceNotFoundException;
import com.smartdesk.mapper.EntityDtoMapper;
import com.smartdesk.repository.CustomerRepository;
import com.smartdesk.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TicketRepository ticketRepository;

    public CustomerService(CustomerRepository customerRepository, TicketRepository ticketRepository) {
        this.customerRepository = customerRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public Customer getCustomerByUserId(UUID userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user ID: " + userId));
    }

    @Transactional(readOnly = true)
    public Customer getCustomerEntity(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerDtoByUserId(UUID userId) {
        return EntityDtoMapper.toCustomerResponse(getCustomerByUserId(userId));
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getCustomerProfile(UUID userId) {
        Customer customer = getCustomerByUserId(userId);
        User user = customer.getUser();

        long total = ticketRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).size();
        long active = ticketRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .filter(t -> t.getStatus() != TicketStatus.RESOLVED && t.getStatus() != TicketStatus.CLOSED)
                .count();

        String firstName = user.getProfile() != null ? user.getProfile().getFirstName() : null;
        String lastName = user.getProfile() != null ? user.getProfile().getLastName() : null;
        String phone = user.getProfile() != null ? user.getProfile().getPhone() : null;
        String avatarUrl = user.getProfile() != null ? user.getProfile().getAvatarUrl() : null;

        return new CustomerProfileResponse(
                customer.getId(),
                user.getId(),
                customer.getCustomerCode(),
                customer.getCompanyName(),
                customer.getPlan(),
                user.getEmail(),
                firstName,
                lastName,
                phone,
                avatarUrl,
                total,
                active
        );
    }
}
