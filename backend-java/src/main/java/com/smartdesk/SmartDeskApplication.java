package com.smartdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SmartDesk Primary Business Backend Application.
 * Core orchestration platform for intelligent ticket routing, customer support,
 * SLA management, and multi-tier escalation.
 */
@SpringBootApplication
public class SmartDeskApplication {

    public static void main(String[] args) {
        if ("Asia/Calcutta".equalsIgnoreCase(java.util.TimeZone.getDefault().getID())) {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
        }
        SpringApplication.run(SmartDeskApplication.class, args);
    }
}
