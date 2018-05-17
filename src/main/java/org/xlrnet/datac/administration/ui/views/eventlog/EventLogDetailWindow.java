package org.xlrnet.datac.administration.ui.views.eventlog;

import org.apache.commons.lang3.StringUtils;
import org.vaadin.viritin.layouts.MVerticalLayout;
import org.xlrnet.datac.commons.ui.DatacTheme;
import org.xlrnet.datac.commons.util.DateTimeUtils;
import org.xlrnet.datac.commons.util.DisplayUtils;
import org.xlrnet.datac.foundation.domain.EventLogMessage;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Window;

/**
 * Window which displays a detailed view of a single eventlog message.
 */
public class EventLogDetailWindow extends Window {

    public EventLogDetailWindow(EventLogMessage message) {
        super("Message details", buildContent(message));
    }

    private static Component buildContent(EventLogMessage message) {
        MVerticalLayout layout = new MVerticalLayout();
        layout.setSizeUndefined();
        layout.addStyleName(DisplayUtils.severityToStyle(message.getSeverity()));

        GridLayout gridLayout = new GridLayout(2, 5);
        gridLayout.setWidth(DatacTheme.FULL_SIZE);

        gridLayout.addComponent(new Label("Time:"));
        gridLayout.addComponent(new Label(DateTimeUtils.format(message.getCreatedAt())));

        gridLayout.addComponent(new Label("Severity:"));
        gridLayout.addComponent(new Label(message.getSeverity().toString()));

        gridLayout.addComponent(new Label("Project:"));
        gridLayout.addComponent(new Label(message.getProjectName()));

        gridLayout.addComponent(new Label("User:"));
        gridLayout.addComponent(new Label(message.getUserName()));

        gridLayout.addComponent(new Label("Short message:"));
        gridLayout.addComponent(new Label(message.getShortMessage()));

        if (!StringUtils.isBlank(message.getDetailedMessage())) {
            gridLayout.addComponent(new Label("Detailed message:"));
            gridLayout.addComponent(new Label(DisplayUtils.convertToHtml(message.getDetailedMessage()), ContentMode.HTML));
        }

        layout.addComponent(gridLayout);
        return layout;
    }
}
