package org.xlrnet.datac.foundation.ui.components;

import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.label.MLabel;
import org.vaadin.viritin.layouts.MHorizontalLayout;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.vaadin.viritin.layouts.MWindow;
import org.xlrnet.datac.commons.ui.DatacTheme;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Modal window with a progress bar.
 */
@ViewScope
@SpringComponent
public class ProgressWindow extends MWindow {

    /**
     * The progress bar which is displayed in the middle of the window.
     */
    private final ProgressBar progressBar = new ProgressBar();

    /**
     * Label with current progress information.
     */
    private final MLabel statusLabel = new MLabel();

    /** Button to close the progress window. */
    private final MButton closeButton = new MButton("Close").withEnabled(false).withStyleName(ValoTheme.BUTTON_PRIMARY).withListener(e -> this.close());

    public ProgressWindow() {
        super();
        setWidth("500px");
        setHeight("200px");
        setResizable(false);
        setClosable(false);
        setModal(true);
        center();
        setContent(buildContent());
    }

    private Component buildContent() {
        MVerticalLayout layout = new MVerticalLayout().withFullSize();
        progressBar.setWidth(DatacTheme.FULL_SIZE);
        progressBar.setHeight("24px");
        MHorizontalLayout buttonBar = new MHorizontalLayout().withStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR).withFullWidth();
        buttonBar.with(closeButton).alignAll(Alignment.BOTTOM_RIGHT);
        layout.with(statusLabel, progressBar, buttonBar);
        layout.withAlign(progressBar, Alignment.MIDDLE_CENTER);
        return layout;
    }

    public void setCloseButtonEnabled(boolean enabled) {
        closeButton.setEnabled(enabled);
    }

    public void updateProgress(float progress) {
        progressBar.setValue(progress);
    }

    public void setMessage(String message) {
        statusLabel.setValue(message);
    }

    public void reset() {
        setCloseButtonEnabled(false);
        progressBar.setValue(0);
        statusLabel.setValue(null);
    }
}
