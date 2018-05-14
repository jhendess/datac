package org.xlrnet.datac.database.domain;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FontIcon;

import lombok.Getter;

/** Enum to differentiate between different types of instances. */
public enum InstanceType {

    /** Virtual and non-persistent instance for which serves as the root of all child instances. */
    ROOT(VaadinIcons.PACKAGE),

    /** Virtual instance used for grouping other instances. */
    GROUP(VaadinIcons.FOLDER),

    /** An actual real instance which can be used to perform deployments. */
    DATABASE(VaadinIcons.DATABASE);

    /** Icon to display for this enum. */
    @Getter
    private final FontIcon iconResource;

    InstanceType(FontIcon iconResource) {
        this.iconResource = iconResource;
    }
}
