package org.xlrnet.datac;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Class containing information about build version, etc..
 */
@Configuration
@PropertySource("classpath:build.properties")
public class BuildInformation {

    @Value("${build.version}")
    private String version;

    @Value("${build.revision}")
    private String revision;

    @Value("${build.timestamp}")
    private String buildTimestamp;

    /**
     * Returns the build version of the application.
     *
     * @return the build version of the application.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the build revision in SCM of the application.
     *
     * @return the build revision in SCM of the application.
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Returns the build timestamp.
     *
     * @return the build timestamp.
     */
    public String getBuildTimestamp() {
        return buildTimestamp;
    }
}
