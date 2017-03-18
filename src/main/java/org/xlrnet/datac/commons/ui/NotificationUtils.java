package org.xlrnet.datac.commons.ui;

import com.vaadin.server.Page;
import com.vaadin.shared.Position;
import com.vaadin.ui.Notification;
import com.vaadin.ui.themes.ValoTheme;

import javax.validation.ConstraintViolation;
import java.util.Iterator;
import java.util.Set;

/**
 * Utilities for showing prestyled notifications.
 */
public class NotificationUtils {

    /**
     * Default time for displaying a notification in case of an error.
     */
    private static final int ERROR_NOTIFICATION_DELAY_MS = 5000;

    /**
     * Message for displaying not implemented.
     */
    private static final String NOT_IMPLEMENTED = "Not implemented.";

    /**
     * Default delay for notifications.
     */
    private static final int DEFAULT_DELAY = 3000;

    private NotificationUtils() {
        // No instantiation possible
    }

    public static void showSuccess(String message) {
        Notification notification = new Notification(message);
        notification.setStyleName(ValoTheme.NOTIFICATION_SUCCESS);
        notification.setDelayMsec(DEFAULT_DELAY);
        applyDefaultAndShow(notification);
    }

    private static void applyDefaultAndShow(Notification notification) {
        notification.setPosition(Position.BOTTOM_RIGHT);
        notification.setDelayMsec(DEFAULT_DELAY);

        notification.show(Page.getCurrent());
    }

    public static void showWarning(String message) {
        Notification notification = new Notification(message);
        notification.setStyleName(ValoTheme.NOTIFICATION_WARNING);
        notification.setDelayMsec(DEFAULT_DELAY);
        applyDefaultAndShow(notification);
    }

    public static void showError(String caption, String message, boolean persistent) {
        Notification notification = new Notification(caption, message);
        notification.setStyleName(ValoTheme.NOTIFICATION_ERROR);
        if (persistent) {
            notification.setDelayMsec(Notification.DELAY_FOREVER);
        } else {
            notification.setDelayMsec(ERROR_NOTIFICATION_DELAY_MS);
        }
        applyDefaultAndShow(notification);
    }

    /**
     * Displays a small notification in the tray.
     *
     * @param message
     *         The message to display.
     */
    public static void showTrayNotification(String message) {
        Notification.show(message, Notification.Type.TRAY_NOTIFICATION);
    }

    public static void showError(String message, boolean persistent) {
        showError(message, null, persistent);
    }

    public static void showNotImplemented() {
        Notification.show(NOT_IMPLEMENTED);
    }

    public static void showValidationError(String s, Set<ConstraintViolation<?>> constraintViolations) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Iterator<ConstraintViolation<?>> iterator = constraintViolations.iterator(); iterator.hasNext(); ) {
            ConstraintViolation<?> constraintViolation = iterator.next();
            stringBuilder.append(constraintViolation.getMessage());
            if (iterator.hasNext()) {
                stringBuilder.append("\n");
            }
        }
        showError(s, stringBuilder.toString(), false);
    }
}
