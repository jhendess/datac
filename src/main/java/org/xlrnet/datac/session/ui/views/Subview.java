package org.xlrnet.datac.session.ui.views;

import com.vaadin.navigator.View;
import com.vaadin.ui.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Interface which must be implemented by all
 */
public interface Subview extends View {

    @NotNull
    Component getContent();
}
