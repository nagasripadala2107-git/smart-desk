package com.smartdesk.config;

import com.smartdesk.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@Profile("!test")
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final DataSource dataSource;
    private final TeamRepository teamRepository;

    public DatabaseSeeder(DataSource dataSource, TeamRepository teamRepository) {
        this.dataSource = dataSource;
        this.teamRepository = teamRepository;
    }

    @Override
    public void run(String... args) {
        try {
            if (teamRepository.count() == 0) {
                log.info("Empty database detected. Applying baseline seed data from seed.sql...");
                try (Connection conn = dataSource.getConnection()) {
                    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                    populator.setContinueOnError(true);
                    populator.setIgnoreFailedDrops(true);
                    populator.addScript(new ClassPathResource("seed.sql"));
                    populator.populate(conn);
                    log.info("Baseline database seed completed successfully.");
                }
            } else {
                log.info("Database already contains data ({} teams registered). Skipping auto-seed.", teamRepository.count());
            }
        } catch (Exception e) {
            log.warn("Database auto-seeding encountered a notice: {}", e.getMessage());
        }
    }
}
