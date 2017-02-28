package org.xlrnet.datac.foundation.ui.views;

import org.jetbrains.annotations.NotNull;

import com.vaadin.navigator.View;
import com.vaadin.ui.Component;

/**
 * Interface which must be implemented by all
 */
public interface Subview extends View {

    @NotNull
    Component getContent();
}
