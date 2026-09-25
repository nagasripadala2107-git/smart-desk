package com.smartdesk.config;

import com.smartdesk.entity.Agent;
import com.smartdesk.entity.Category;
import com.smartdesk.entity.Customer;
import com.smartdesk.entity.Profile;
import com.smartdesk.entity.Team;
import com.smartdesk.entity.User;
import com.smartdesk.entity.enums.AgentAvailability;
import com.smartdesk.entity.enums.UserRole;
import com.smartdesk.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@org.springframework.context.annotation.Profile("!test")
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final TeamRepository teamRepository;
    private final CategoryRepository categoryRepository;
    private final AgentRepository agentRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            TeamRepository teamRepository,
            CategoryRepository categoryRepository,
            AgentRepository agentRepository,
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.teamRepository = teamRepository;
        this.categoryRepository = categoryRepository;
        this.agentRepository = agentRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            seedTeamsAndCategories();
            seedDemoUsers();
        } catch (Exception e) {
            log.warn("Database auto-seeding encountered an issue: {}", e.getMessage(), e);
        }
    }

    private void seedTeamsAndCategories() {
        if (teamRepository.count() == 0) {
            log.info("Seeding baseline teams...");
            teamRepository.saveAll(List.of(
                    new Team("General Support", "First-line triage and general inquiries team"),
                    new Team("Billing", "Handles subscriptions, invoicing, and payment inquiries"),
                    new Team("Technical Support", "Handles technical defects, server diagnostics, and code issues"),
                    new Team("Security", "Handles security vulnerabilities, incident response, and compliance"),
                    new Team("Senior Support", "Tier 2 escalation team for complex technical issues"),
                    new Team("Management", "Executive oversight and high-priority escalation resolution")
            ));
        }

        if (categoryRepository.count() == 0) {
            log.info("Seeding baseline categories...");
            categoryRepository.saveAll(List.of(
                    new Category("BILLING", "Billing, invoices, and charge inquiries"),
                    new Category("TECHNICAL", "Technical infrastructure and platform usage issues"),
                    new Category("ACCOUNT", "Account access, credential reset, and permissions"),
                    new Category("REFUND", "Refund requests and payment disputes"),
                    new Category("SECURITY", "Security reports, potential compromises, and audit questions"),
                    new Category("SUBSCRIPTION", "Plan changes, upgrades, and renewals"),
                    new Category("BUG", "Software anomalies, errors, and unexpected behavior"),
                    new Category("FEATURE_REQUEST", "Product enhancements and feature suggestions"),
                    new Category("OTHER", "Unclassified questions and general inquiries")
            ));
        }
    }

    private void seedDemoUsers() {
        String defaultPasswordHash = passwordEncoder.encode("Password123!");

        // 1. Admin
        if (!userRepository.existsByEmail("admin@smartdesk.local")) {
            log.info("Seeding default admin user admin@smartdesk.local...");
            User admin = new User("admin@smartdesk.local", defaultPasswordHash, UserRole.ADMIN);
            admin = userRepository.save(admin);
            profileRepository.save(new Profile(admin, "System", "Administrator", "+1-555-0100", null));
        }

        // 2. Tech Support Agent
        if (!userRepository.existsByEmail("agent.tech@smartdesk.local")) {
            log.info("Seeding default agent user agent.tech@smartdesk.local...");
            User agentUser = new User("agent.tech@smartdesk.local", defaultPasswordHash, UserRole.AGENT);
            agentUser = userRepository.save(agentUser);
            profileRepository.save(new Profile(agentUser, "Bob", "Technician", "+1-555-0103", null));

            Team techTeam = teamRepository.findByName("Technical Support").orElse(null);
            agentRepository.save(new Agent(agentUser, techTeam, "EMP-TEC-102", AgentAvailability.AVAILABLE, "java, spring, postgresql, nextjs", 8));
        }

        // 3. Customer
        if (!userRepository.existsByEmail("alex@acmecorp.local")) {
            log.info("Seeding default customer user alex@acmecorp.local...");
            User customerUser = new User("alex@acmecorp.local", defaultPasswordHash, UserRole.CUSTOMER);
            customerUser = userRepository.save(customerUser);
            profileRepository.save(new Profile(customerUser, "Alex", "Acme", "+1-555-0201", null));
            customerRepository.save(new Customer(customerUser, "CUST-ACME-001", "Acme Corporation", "ENTERPRISE"));
        }
    }
}
