package org.xlrnet.datac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.xlrnet.datac.administration.domain.User;
import org.xlrnet.datac.session.services.UserService;

import java.util.Locale;

/**
 * Main application class for bootstrapping.
 */
@SpringBootApplication
public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static final String APPLICATION_NAME = "Datac";

    private final BuildInformation buildInformation;

    private final UserService userService;

    @Autowired
    public Application(BuildInformation buildInformation, UserService userService) {
        this.buildInformation = buildInformation;
        this.userService = userService;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner startupLogging() {
        return strings -> {
            LOGGER.info("Starting {} version {}", APPLICATION_NAME, buildInformation.getVersion(), buildInformation.getVersion());

            Locale.setDefault(Locale.ENGLISH);  // Reset the locale VM-wide

            User user = new User();
            user.setFirstName("Internal");
            user.setLastName("User");
            user.setLoginName("system");
            user.setEmail("system@demo.org");
            userService.createNewUser(user, "system");
            LOGGER.info("Created technical user system:system");
        };
    }
}
