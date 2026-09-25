package com.smartdesk.repository;

import com.smartdesk.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByUserId(UUID userId);
    Optional<Customer> findByCustomerCode(String customerCode);
    boolean existsByCustomerCode(String customerCode);
}
