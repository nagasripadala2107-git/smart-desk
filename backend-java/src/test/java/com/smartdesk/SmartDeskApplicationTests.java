package com.smartdesk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SmartDeskApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the entire Spring application context starts cleanly
    }
}
