package org.xlrnet.datac.administration.ui.views.eventlog;

import com.vaadin.data.provider.QuerySortOrder;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.VerticalLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.xlrnet.datac.commons.util.QueryUtils;
import org.xlrnet.datac.foundation.domain.EventLogMessage;
import org.xlrnet.datac.foundation.services.EventLogService;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Stream;

/**
 * Re-usable layout which displays events.
 */
@Component
@UIScope
public class EventLogLayout extends VerticalLayout {

    private final EventLogService eventLogService;

    @Autowired
    public EventLogLayout(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @PostConstruct
    public void init() {
        setWidth("100%");

        Grid<EventLogMessage> logMessageGrid = new Grid<>();
        logMessageGrid.setWidth("80%");
        logMessageGrid.setResponsive(true);

        logMessageGrid.addColumn(EventLogMessage::getCreatedAt)
                .setSortProperty("createdAt")
                .setMaximumWidth(235)
                .setCaption("Time");
        logMessageGrid.addColumn(EventLogMessage::getSeverity)
                .setCaption("Severity")
                .setMaximumWidth(115)
                .setSortProperty("severity");       // TODO: Convert to icon
        logMessageGrid.addColumn(EventLogMessage::getProjectName)
                .setCaption("Project")
                .setMaximumWidth(130)
                .setSortProperty("eventLog.project.name");
        logMessageGrid.addColumn(EventLogMessage::getUserName)
                .setCaption("User")
                .setMaximumWidth(130)
                .setSortProperty("eventLog.user.loginName");
        logMessageGrid.addColumn(EventLogMessage::getShortMessage)
                .setCaption("Short message")
                .setSortProperty("shortMessage");

        logMessageGrid.setStyleGenerator(m -> {
            String style = null;
            switch (m.getSeverity()) {
                case ERROR:
                    style = "severity-error";
                    break;
                case WARNING:
                    style = "severity-warning";
                    break;
            }
            return style;
        });

        logMessageGrid.setDataProvider(
                new FetchEventLogMessagesCallback(),
                () -> Math.toIntExact(eventLogService.countAll())
        );

        Button refreshButton = new Button("Refresh");
        refreshButton.setIcon(VaadinIcons.REFRESH);
        refreshButton.addClickListener(e -> logMessageGrid.getDataProvider().refreshAll());

        addComponent(refreshButton);
        addComponent(logMessageGrid);
    }

    private class FetchEventLogMessagesCallback implements Grid.FetchItemsCallback<EventLogMessage> {

        @Override
        public Stream<EventLogMessage> fetchItems(List<QuerySortOrder> sortOrder, int offset, int limit) {
            Sort querySortOrder = null;
            if (sortOrder != null && !sortOrder.isEmpty()) {
                querySortOrder = QueryUtils.convertVaadinToSpringSort(sortOrder);
            }
            return eventLogService.findAllMessagesPaged(limit, offset, querySortOrder).stream();
        }
    }
}
