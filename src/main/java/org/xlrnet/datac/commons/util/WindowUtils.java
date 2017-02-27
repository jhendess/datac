package org.xlrnet.datac.commons.util;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Utility methods for quickly displaying windows.
 */
public class WindowUtils {

    private static final String DEFAULT_OK = "OK";

    private WindowUtils() {

    }

    /**
     * Displays a modal popup dialog with only a single "OK" button and a given text. The window will immediately open
     * after calling the method. The window closes when clicking the button.
     *
     * @param title
     *         Title of the modal window.
     * @param htmlContent
     *         HTML content of the window.
     * @return the created window.
     */
    public static Window showModalDialog(String title, String htmlContent) {
        return showModalDialog(title, new Label(htmlContent, ContentMode.HTML));
    }

    /**
     * Displays a modal popup dialog with only a single "OK" button and a custom content. The window will immediately
     * open after calling the method. The window closes when clicking the button.
     *
     * @param title
     *         Title of the modal window.
     * @param component
     *         Custom component to display.
     * @return the created window.
     */
    public static Window showModalDialog(String title, Component component) {
        Window window = new Window(title);
        window.setModal(true);

        VerticalLayout layout = new VerticalLayout();
        layout.addComponent(component);
        layout.setComponentAlignment(component, Alignment.MIDDLE_CENTER);
        layout.setSizeUndefined();
        layout.setMargin(true);

        Button okButton = new Button(DEFAULT_OK);
        okButton.setStyleName(ValoTheme.BUTTON_PRIMARY);
        okButton.addClickListener(e -> window.close());

        layout.addComponent(okButton);
        layout.setComponentAlignment(okButton, Alignment.BOTTOM_CENTER);

        window.setContent(layout);
        UI.getCurrent().addWindow(window);
        return window;
    }

}
