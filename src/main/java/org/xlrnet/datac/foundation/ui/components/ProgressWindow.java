package org.xlrnet.datac.foundation.ui.components;

import org.vaadin.viritin.label.MLabel;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.vaadin.viritin.layouts.MWindow;
import org.xlrnet.datac.commons.ui.DatacTheme;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.ProgressBar;

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

    public ProgressWindow() {
        super();
        setWidth("500px");
        setHeight("150px");
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
        layout.with(statusLabel, progressBar);
        layout.withAlign(progressBar, Alignment.BOTTOM_CENTER);
        return layout;
    }

    public void updateProgress(float progress) {
        progressBar.setValue(progress);
    }

    public void setMessage(String message) {
        statusLabel.setValue(message);
    }

    public void reset() {
        progressBar.setValue(0);
        statusLabel.setValue(null);
    }
}
