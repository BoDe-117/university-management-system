package com.abdulrahman.university_management_system.security.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("bootstrap-admin")
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminBootstrapService bootstrapService;
    private final String username;
    private final String password;

    public AdminBootstrapRunner(
            AdminBootstrapService bootstrapService,
            @Value("${BOOTSTRAP_ADMIN_USERNAME:}") String username,
            @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password
    ) {
        this.bootstrapService = bootstrapService;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean created = bootstrapService.createFirstAdmin(username, password);

        if (created) {
            log.info("Initial administrator account created.");
        } else {
            log.info("Initial administrator already exists; bootstrap skipped.");
        }
    }
}
