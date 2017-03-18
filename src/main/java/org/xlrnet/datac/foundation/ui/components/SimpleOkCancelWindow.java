package org.xlrnet.datac.foundation.ui.components;

import com.vaadin.shared.Registration;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.jetbrains.annotations.NotNull;

/**
 * Simple modal window with a primary "OK" button and a "Cancel" button. The cancel button will close the window by
 * default unless another action is specified.
 */
public class SimpleOkCancelWindow extends Window {

    private static final String CANCEL_TEXT = "Cancel";

    private static final String OK_TEXT = "OK";

    private final VerticalLayout mainLayout;

    private final HorizontalLayout buttonLayout;

    private final Layout customContentLayout;

    private final Button okButton;

    private final Button cancelButton;

    private Registration okHandlerRegistration;

    private Registration cancelButtonListenerRegistration;

    private Registration closeListenerRegistration;

    public SimpleOkCancelWindow(String windowTitle, Component customContent, String okButtonCaption, String cancelButtonCaption) {
        super(windowTitle);
        mainLayout = new VerticalLayout();
        customContentLayout = new VerticalLayout();
        buttonLayout = new HorizontalLayout();

        okButton = new Button(okButtonCaption != null ? okButtonCaption : OK_TEXT);
        okButton.setStyleName(ValoTheme.BUTTON_PRIMARY);
        cancelButton = new Button(cancelButtonCaption != null ? cancelButtonCaption : CANCEL_TEXT);
        cancelButton.addClickListener(e -> close());

        buttonLayout.addComponent(okButton);
        buttonLayout.addComponent(cancelButton);

        mainLayout.addComponent(customContentLayout);
        mainLayout.addComponent(buttonLayout);
        mainLayout.setComponentAlignment(buttonLayout, Alignment.BOTTOM_CENTER);

        if (customContent != null) {
            setCustomContent(customContent);
        }

        setModal(true);
        setContent(mainLayout);
    }

    public SimpleOkCancelWindow(String windowTitle, String okButtonCaption, String cancelButtonCaption) {
        this(windowTitle, null, okButtonCaption, cancelButtonCaption);
    }

    public SimpleOkCancelWindow(String windowCaption) {
        this(windowCaption, OK_TEXT, CANCEL_TEXT);
    }

    public SimpleOkCancelWindow() {
        this("");
    }

    /**
     * Sets the main content of this window which will be displayed above the buttons. If there is already a component
     * attached, it will be removed.
     *
     * @param component
     *         The component to use as the main component.
     */
    public void setCustomContent(@NotNull Component component) {
        customContentLayout.removeAllComponents();
        customContentLayout.addComponent(component);
    }

    /**
     * Set a custom handler for the OK button. This will overwrite the existing handler.
     *
     * @param okHandler
     *         The handler to bind.
     */
    public void setOkHandler(@NotNull GenericHandler okHandler) {
        if (okHandlerRegistration != null) {
            okHandlerRegistration.remove();
        }
        okHandlerRegistration = okButton.addClickListener(e -> okHandler.handle());
    }

    /**
     * Set a custom handler for the cancel button and for closing the window. By default the window just silently
     * closes. This will overwrite the existing handler.
     *
     * @param cancelHandler
     *         The handler to bind.
     */
    public void setCancelHandler(@NotNull GenericHandler cancelHandler) {
        if (cancelButtonListenerRegistration != null) {
            cancelButtonListenerRegistration.remove();
        }
        if (closeListenerRegistration != null) {
            closeListenerRegistration.remove();
        }

        cancelButtonListenerRegistration = cancelButton.addClickListener(e -> cancelHandler.handle());
        closeListenerRegistration = addCloseListener(e -> cancelHandler.handle());
    }
}
