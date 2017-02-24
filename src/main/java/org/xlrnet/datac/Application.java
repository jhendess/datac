package org.xlrnet.datac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for bootstrapping.
 */
@SpringBootApplication
public class Application {

    public static final String APPLICATION_NAME = "Datac";

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
