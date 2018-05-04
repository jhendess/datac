package org.xlrnet.datac.database.domain;

/**
 * Virtual deployment instance which matches the root project.
 */
public class DeploymentRoot implements IDatabaseInstance {

    private final String name;

    public DeploymentRoot(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isGroup() {
        return true;
    }
}
