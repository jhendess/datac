package org.xlrnet.datac;

import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.scheduling.annotation.EnableAsync;
import org.xlrnet.datac.foundation.configuration.BuildInformation;
import org.xlrnet.datac.foundation.domain.EventType;
import org.xlrnet.datac.foundation.domain.MessageSeverity;
import org.xlrnet.datac.foundation.services.EventLogService;
import org.xlrnet.datac.session.domain.User;
import org.xlrnet.datac.session.services.UserService;

/**
 * Main application class for bootstrapping.
 */
@EnableAsync
@SpringBootApplication
@EntityScan(basePackageClasses = {
        Application.class,
        Jsr310JpaConverters.class // Enable new JSR-310 time conversions for hibernate
})
public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static final String APPLICATION_NAME = "Datac";

    private final BuildInformation buildInformation;

    private final UserService userService;

    private final EventLogService eventLogService;

    @Autowired
    public Application(BuildInformation buildInformation, UserService userService, EventLogService eventLogService) {
        this.buildInformation = buildInformation;
        this.userService = userService;
        this.eventLogService = eventLogService;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner startupLogging() {
        return strings -> {
            LOGGER.info("Starting {} version {}", APPLICATION_NAME, buildInformation.getVersion(), buildInformation.getVersion());

            Locale.setDefault(Locale.ENGLISH);  // Reset the locale VM-wide

            if (userService.findFirstByLoginNameIgnoreCase("system") == null) {
                User user = new User();
                user.setFirstName("Internal");
                user.setLastName("User");
                user.setLoginName("system");
                user.setEmail("system@demo.org");
                Optional<User> systemUser = userService.createNewUser(user, "Sys123");
                systemUser.ifPresent(user1 -> LOGGER.info("Created technical user system:Sys123"));
            }

            eventLogService.logSimpleEventMessage(EventType.STARTUP, MessageSeverity.INFO, "Application startup complete");
        };
    }
}
