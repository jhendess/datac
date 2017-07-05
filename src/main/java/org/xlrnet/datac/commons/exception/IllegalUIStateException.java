package org.xlrnet.datac.commons.exception;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * The UI encountered an illegal state. This may happen if a URL was called with an illegal parameter.
 */
public class IllegalUIStateException extends DatacRuntimeException {

    private final String viewName;

    private final String[] parameters;

    public IllegalUIStateException(@NotNull String viewName, @NotNull String... parameters) {
        super("Illegal state while trying to render view " + viewName + " with parameters " + Arrays.toString(parameters));
        this.viewName = viewName;
        this.parameters = parameters;
    }

    @NotNull
    public String getViewName() {
        return viewName;
    }

    @NotNull
    public String[] getParameters() {
        return parameters;
    }
}
